package com.vickezi.processor.executions;
import com.vickezi.processor.dao.ReactiveDatabaseService;
import com.vickezi.processor.util.MultiTenantFileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
@Service
public class MultiTenantTerraformService {
    private final Logger logger = LoggerFactory.getLogger(MultiTenantTerraformService.class);
    @Value("${terraform.base.directory}") String baseDirectory="C:\\project\\university-portal\\terraform";
    @Value("${terraform.binary.path}") String terraformBinaryPath;
    @Value("${terraform.default.config:provider \"null\" {}}") String defaultConfig;
    @Value("${aws.s3.bucket}") String s3Bucket;
    private final S3AsyncClient s3Client;
    private final DynamoDbAsyncClient dynamoDbClient;
    private final  DatabaseClient dbClient;
    private final ReactiveDatabaseService databaseService;
    final MultiTenantFileManager fileManager;
    private static final long LOCK_TTL_SECONDS = 1800; // 30 minutes
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(2);

    public MultiTenantTerraformService(S3AsyncClient s3Client,
                                       DynamoDbAsyncClient dynamoDbClient,
                                       DatabaseClient dbClient,
                                       ReactiveDatabaseService databaseService
    ) {
        this.s3Client = s3Client;
        this.dynamoDbClient = dynamoDbClient;
        this.dbClient = dbClient;
        this.databaseService = databaseService;
        this.fileManager = new MultiTenantFileManager(baseDirectory);

    }
    public Mono<String> executeTerraformCommand(String tenantId, UUID templateId, String userId,
                                                String command, String... args) {
        return acquireLock(tenantId, templateId, userId)
                .then(fileManager.getTenantDirectory(tenantId)) // Ensure directory exists first
                .flatMap(tenantDir -> databaseService.fetchTemplateMetadata(tenantId, templateId)
                        .flatMap(template -> fetchTerraformFile(template.s3Key())
                                .switchIfEmpty(Mono.just(defaultConfig))
                                .flatMap(content -> fileManager.writeFile(tenantId, "main.tf", content)))
                        .thenReturn(tenantDir))
                .flatMap(tenantDir -> runTerraformCommand(tenantId, tenantDir, templateId, userId, command, args))
                .flatMap(output -> releaseLock(tenantId, templateId).thenReturn(output))
                .timeout(Duration.ofMinutes(30))
                .publishOn(Schedulers.boundedElastic())
                .onErrorMap(ex -> {
                    releaseLock(tenantId,templateId).subscribe();
                   return new RuntimeException("Terraform execution failed for tenant " + tenantId, ex);
                });
    }

    private Mono<String> fetchTerraformFile(String s3Key) {
        return Mono.fromFuture(s3Client.getObject(
                        GetObjectRequest.builder().bucket(s3Bucket).key(s3Key).build(),
                        software.amazon.awssdk.core.async.AsyncResponseTransformer.toBytes()))
                .map(response -> new String(response.asByteArray()))
                .onErrorResume(ex -> Mono.empty()); // Return empty to trigger default config
    }

    private Mono<String> runTerraformCommand(String tenantId, Path workingDir, UUID templateId, String userId,
                                             String command, String[] args) {
        return Mono.fromCallable(() -> {
                    String[] fullCommand = buildCommandArray(command, args);
                    ProcessBuilder processBuilder = new ProcessBuilder(fullCommand);
                    processBuilder.directory(workingDir.toFile());
                    processBuilder.redirectErrorStream(true);

                    Process process = processBuilder.start();
                    StringBuilder output = new StringBuilder();

                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            output.append(line).append("\n");
                        }
                    }

                    int exitCode = process.waitFor();
                    String result = output.toString();

                    if (exitCode != 0) {
                        throw new IOException("Terraform command failed with exit code " + exitCode + ": " + result);
                    }
                    return result;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(output -> {
                    Mono<Void> stateHandling = command.equals("init")
                            ? Mono.empty() // Skip state handling for init
                            : Mono.fromCallable(() -> Files.exists(workingDir.resolve("terraform.tfstate"))
                                    ? Files.readString(workingDir.resolve("terraform.tfstate"))
                                    : "")
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(state -> fileManager.writeFile(tenantId, "terraform.tfstate", state));
                    return logExecution(tenantId, templateId, userId, command, output)
                            .then(stateHandling)
                            .thenReturn(output);
                });
    }
    private Mono<Void> logExecution(String tenantId, UUID templateId, String userId, String command, String output) {
        return dbClient.sql("INSERT INTO execution_history (id, template_id, tenant_id, executed_by, command, output, started_at, completed_at) " +
                        "VALUES (gen_random_uuid(), :templateId, :tenantId, :userId, :command, :output, :startedAt, :completedAt)")
                .bind("templateId", templateId)
                .bind("tenantId", tenantId)
                .bind("userId", userId)
                .bind("command", command)
                .bind("output", output)
                .bind("startedAt", Instant.now())
                .bind("completedAt", Instant.now())
                .then();
    }

    private Mono<Void> acquireLock(String tenantId, UUID templateId, String userId) {
        String lockId = tenantId + "-" + templateId.toString();
        long currentTime = Instant.now().getEpochSecond();
        long ttl = currentTime + LOCK_TTL_SECONDS;

        Map<String, AttributeValue> item = Map.of(
                "Application", AttributeValue.builder().s(lockId).build(),
                "lockedAt", AttributeValue.builder().n(String.valueOf(currentTime)).build(),
                "ttl", AttributeValue.builder().n(String.valueOf(ttl)).build(),
                "owner", AttributeValue.builder().s(userId).build()
        );

        // Expression attribute names mapping to avoid reserved word conflict
        Map<String, String> expressionAttributeNames = Map.of(
                "#ttl", "ttl" // Use #ttl to refer to the ttl field
        );

        PutItemRequest request = PutItemRequest.builder()
                .tableName("TerraformLocks")
                .item(item)
                .conditionExpression("attribute_not_exists(Application) OR #ttl < :currentTime") // Using #ttl
                .expressionAttributeNames(expressionAttributeNames) // Map for reserved words
                .expressionAttributeValues(Map.of(
                        ":currentTime", AttributeValue.builder().n(String.valueOf(currentTime)).build()
                ))
                .build();

        return Mono.fromFuture(() -> dynamoDbClient.putItem(request))
                .retryWhen(reactor.util.retry.Retry.backoff(MAX_RETRY_ATTEMPTS, RETRY_DELAY)
                        .filter(throwable -> throwable instanceof ConditionalCheckFailedException)
                        .doBeforeRetry(signal -> System.out.println("Retrying lock acquisition for " + lockId)))
                .onErrorMap(ConditionalCheckFailedException.class,
                        ex -> new RuntimeException("Lock already acquired for template " + templateId))
                .doOnError(error -> {
                    // Log additional information on error
                    System.err.println("Failed to acquire lock for " + lockId + ": " + error.getMessage());
                })
                .then(Mono.fromRunnable(()->logger.warn("Acquired lock ID [{}]", lockId)));
    }

    private Mono<Void> releaseLock(String tenantId, UUID templateId) {
        String lockId = tenantId + "-" + templateId.toString();
        DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName("TerraformLocks")
                .key(Map.of("Application", AttributeValue.builder().s(lockId).build()))
                .build();
        logger.warn("Releasing lock ID [{}]", lockId);
        return Mono.fromFuture(() -> dynamoDbClient.deleteItem(request))
                .onErrorResume(ex -> {
                    System.err.println("Failed to release lock for " + lockId + ": " + ex.getMessage());
                    return Mono.empty();
                })
                .then();
    }

    private String[] buildCommandArray(String command, String[] args) {
        String[] baseCommand = {terraformBinaryPath, command};
        String[] result = new String[baseCommand.length + args.length];
        System.arraycopy(baseCommand, 0, result, 0, baseCommand.length);
        System.arraycopy(args, 0, result, baseCommand.length, args.length);
        return result;
    }
}