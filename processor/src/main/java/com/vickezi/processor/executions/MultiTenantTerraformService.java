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
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class MultiTenantTerraformService {
    private final Logger logger = LoggerFactory.getLogger(MultiTenantTerraformService.class);
    private final ReactiveDatabaseService reactiveDatabaseService;
    private String baseDirectory;

    @Value("${terraform.binary.path}")
    private String terraformBinaryPath;

    @Value("${terraform.default.config:provider \"null\" {}}")
    private String defaultConfig;

    @Value("${aws.s3.bucket}")
    private String s3Bucket;

    private final S3AsyncClient s3Client;
    private final DynamoDbAsyncClient dynamoDbClient;
    private final DatabaseClient dbClient;
    private final MultiTenantFileManager fileManager;

    private static final long LOCK_TTL_SECONDS = 1800; // 30 minutes
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(2);

    public MultiTenantTerraformService(
            S3AsyncClient s3Client,
            DynamoDbAsyncClient dynamoDbClient,
            DatabaseClient dbClient,
            @Value("${terraform.base.directory}") String baseDirectory, ReactiveDatabaseService reactiveDatabaseService) {
        this.baseDirectory = baseDirectory;
        this.s3Client = s3Client;
        this.dynamoDbClient = dynamoDbClient;
        this.dbClient = dbClient;
        this.fileManager = new MultiTenantFileManager(baseDirectory);
        this.reactiveDatabaseService = reactiveDatabaseService;
    }

    public Mono<String> executeTerraformCommand(String tenantId, UUID templateId, String userId,
                                                String command, String... args) {
        return acquireLock(tenantId, templateId, userId)
                .then(fileManager.getTenantDirectory(tenantId))
                .flatMap(tenantDir -> {
                    String backendConfig = String.format(
                            "terraform {\n" +
                                    "  backend \"s3\" {\n" +
                                    "    bucket         = \"%s\"\n" +
                                    "    key            = \"%s/%s/terraform.tfstate\"\n" +
                                    "    region         = \"us-east-1\"\n" +
                                    "    dynamodb_table = \"TerraformLocks\"\n" +
                                    "  }\n" +
                                    "}", s3Bucket, tenantId, templateId);
                    return fileManager.writeFile(tenantId, "backend.tf", backendConfig)
                            .then(reactiveDatabaseService.fetchTemplateMetadata(tenantId, templateId))
                            .flatMap(template -> fetchTerraformFile(template.s3Key())
                                    .switchIfEmpty(Mono.just(defaultConfig))
                                    .flatMap(content -> fileManager.writeFile(tenantId, "main.tf", content)))
                            .thenReturn(tenantDir);
                })
                .flatMap(tenantDir -> runTerraformCommand(tenantId, tenantDir, templateId, userId, command, args))
                .flatMap(output -> releaseLock(tenantId, templateId).thenReturn(output))
                .timeout(Duration.ofMinutes(30))
                .onErrorResume(ex -> releaseLock(tenantId, templateId)
                        .then(Mono.error(new RuntimeException("Terraform execution failed for tenant " + tenantId, ex))));
    }

    private Mono<String> fetchTerraformFile(String s3Key) {
        return Mono.fromFuture(s3Client.getObject(
                        GetObjectRequest.builder().bucket(s3Bucket).key(s3Key).build(),
                        software.amazon.awssdk.core.async.AsyncResponseTransformer.toBytes()))
                .map(response -> new String(response.asByteArray()))
                .onErrorResume(ex -> {
                    logger.warn("Failed to fetch Terraform file from S3 key {}: {}", s3Key, ex.getMessage());
                    return Mono.empty();
                });
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
                        throw new IOException("Terraform command '" + command + "' failed with exit code " + exitCode + ": " + result);
                    }
                    return result;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(output -> {
                    Mono<Void> stateHandling = command.equals("init")
                            ? Mono.empty()
                            : Mono.fromCallable(() -> Files.exists(workingDir.resolve("terraform.tfstate"))
                                    ? Files.readString(workingDir.resolve("terraform.tfstate"))
                                    : "")
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(state -> fileManager.writeFile(tenantId, "terraform.tfstate", state));
                    return reactiveDatabaseService.logExecution(tenantId, templateId, userId, command, output)
                            .then(stateHandling)
                            .thenReturn(output);
                });
    }
    private Mono<Void> acquireLock(String tenantId, UUID templateId, String userId) {
        String lockId = tenantId + "-" + templateId.toString();
        long currentTime = Instant.now().getEpochSecond();
        long ttl = currentTime + LOCK_TTL_SECONDS;

        Map<String, AttributeValue> item = Map.of(
                "lockId", AttributeValue.builder().s(lockId).build(), // Corrected key name
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
                .conditionExpression("attribute_not_exists(lockId) OR #ttl < :currentTime") // Using #ttl
                .expressionAttributeNames(expressionAttributeNames) // Map for reserved words
                .expressionAttributeValues(Map.of(
                        ":currentTime", AttributeValue.builder().n(String.valueOf(currentTime)).build()
                ))
                .build();

        return Mono.fromFuture(() -> dynamoDbClient.putItem(request))
                .retryWhen(reactor.util.retry.Retry.backoff(MAX_RETRY_ATTEMPTS, RETRY_DELAY)
                        .filter(throwable -> throwable instanceof ConditionalCheckFailedException)
                        .doBeforeRetry(signal -> logger.info("Retrying lock acquisition for {}", lockId)))
                .onErrorMap(ConditionalCheckFailedException.class,
                        ex -> new RuntimeException("Lock already acquired for template " + templateId, ex))
                .doOnSuccess(v -> logger.info("Acquired lock ID [{}]", lockId))
                .then();
    }

    private Mono<Void> releaseLock(String tenantId, UUID templateId) {
        String lockId = tenantId + "-" + templateId.toString();
        DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName("TerraformLocks")
                .key(Map.of("lockId", AttributeValue.builder().s(lockId).build()))
                .build();

        return Mono.fromFuture(() -> dynamoDbClient.deleteItem(request))
                .doOnSuccess(v -> logger.info("Released lock ID [{}]", lockId))
                .onErrorResume(ex -> {
                    logger.error("Failed to release lock for {}: {}", lockId, ex.getMessage());
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