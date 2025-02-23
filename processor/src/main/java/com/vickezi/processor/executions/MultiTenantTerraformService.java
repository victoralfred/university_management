package com.vickezi.processor.executions;
import com.vickezi.processor.dao.ReactiveDatabaseService;
import com.vickezi.processor.util.MultiTenantFileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MultiTenantTerraformService {
    private static final Logger logger = LoggerFactory.getLogger(MultiTenantTerraformService.class);

    // Constants
    private static final String S3_REGION = "us-east-1"; // Configurable in production
    private static final String TF_STATE_FILE = "terraform.tfstate";
    private static final String LOCK_TABLE = "TerraformLocks";
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(2);
    private static final Duration MAX_BACKOFF = Duration.ofSeconds(10);
    private static final Set<String> ALLOWED_COMMANDS = Set.of("init", "apply", "destroy", "plan");
    @Value("${terraform.lock.ttl.seconds:300}") // 5 minutes for testing
    private long lockTtlSeconds;
    private final ReactiveDatabaseService reactiveDatabaseService;
    private final S3AsyncClient s3Client;
    private final DynamoDbAsyncClient dynamoDbClient;
    private final MultiTenantFileManager fileManager;

    @Value("${terraform.binary.path}")
    private String terraformBinaryPath;
    @Value("${terraform.default.config:provider \"null\" {}}")
    private String defaultConfig;
    @Value("${aws.s3.bucket}")
    private String s3Bucket;


    public MultiTenantTerraformService(
            S3AsyncClient s3Client,
            DynamoDbAsyncClient dynamoDbClient,
            @Value("${terraform.base.directory}") String baseDirectory,
            ReactiveDatabaseService reactiveDatabaseService) {
        this.s3Client = s3Client;
        this.dynamoDbClient = dynamoDbClient;
        this.fileManager = new MultiTenantFileManager(baseDirectory);
        this.reactiveDatabaseService = reactiveDatabaseService;
    }

    public Mono<String> executeTerraformCommand(String tenantId, UUID templateId, String userId,
                                                String command, String... args) {
        if (!ALLOWED_COMMANDS.contains(command)) {
            return Mono.error(new IllegalArgumentException("Invalid Terraform command: " + command));
        }
        logger.info("Starting Terraform command {} for tenant {}, template {}", command, tenantId, templateId);
        return acquireLock(tenantId, templateId, userId)
                .then(prepareTerraformFiles(tenantId, templateId))
                .publishOn(Schedulers.boundedElastic())
                .flatMap(tenantDir -> {
                    try {
                        logger.debug("Tenant directory before command: {}", Files.list(tenantDir).map(Path::toString)
                                .collect(Collectors.joining(", ")));
                    } catch (IOException e) {
                        return Mono.error(new RuntimeException(e));
                    }
                    return runTerraformCommand(tenantId, tenantDir, templateId, userId, command, args)
                            .doOnSuccess(output -> logger.debug("Command {} output: {}", command, output));
                })
                .flatMap(output -> releaseLock(tenantId, templateId)
                        .doOnSuccess(v -> logger.info("Lock released successfully for tenant {}, template {}", tenantId, templateId))
                        .thenReturn(output))
                .timeout(Duration.ofMinutes(30))
                .onErrorResume(ex -> releaseLock(tenantId, templateId)
                        .doOnSuccess(v -> logger.info("Lock released on error for tenant {}, template {}", tenantId, templateId))
                        .doOnError(err -> logger.error("Failed to release lock on error: {}", err.getMessage()))
                        .then(Mono.error(new RuntimeException("Terraform execution failed for tenant " + tenantId +
                                (ex != null ? " (timed out)" : ""), ex))))
                .doOnSuccess(output -> logger.info("Completed Terraform command {} with output: {}", command, output));
    }

    private Mono<Path> prepareTerraformFiles(String tenantId, UUID templateId) {
        return fileManager.getTenantDirectory(tenantId)
                .flatMap(tenantDir -> {
                    String backendConfig = String.format(
                            "terraform {\n" +
                                    "  backend \"s3\" {\n" +
                                    "    bucket         = \"%s\"\n" +
                                    "    key            = \"%s/%s/%s\"\n" +
                                    "    region         = \"%s\"\n" +
                                    "    dynamodb_table = \"TerraformStateLocks\"\n" +
                                    "  }\n" +
                                    "}", s3Bucket, tenantId, templateId, TF_STATE_FILE, S3_REGION);
                    return fileManager.writeFile(tenantId, "backend.tf", backendConfig)
                            .then(reactiveDatabaseService.fetchTemplateMetadata(tenantId, templateId))
                            .flatMap(template -> fetchTerraformFile(template.s3Key())
                                    .switchIfEmpty(Mono.just(defaultConfig))
                                    .flatMap(content -> fileManager.writeFile(tenantId, "main.tf", content)))
                            .thenReturn(tenantDir);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
    private Mono<String> fetchTerraformFile(String s3Key) {
        return Mono.fromFuture(s3Client.getObject(
                        GetObjectRequest.builder().bucket(s3Bucket).key(s3Key).build(),
                        software.amazon.awssdk.core.async.AsyncResponseTransformer.toBytes()))
                .map(response -> new String(response.asByteArray()))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(ex -> {
                    logger.warn("Failed to fetch Terraform file from S3 key {}: {}", s3Key, ex.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<String> runTerraformCommand(String tenantId, Path workingDir, UUID templateId, String userId,
                                             String command, String[] args) {
        final Instant startedExecution = Instant.now();
        return Mono.fromCallable(() -> {
                    String[] fullCommand = buildCommandArray(command, args);
                    logger.info("Executing Terraform command for tenant {}: {}", tenantId, String.join(" ", fullCommand));
                    ProcessBuilder processBuilder = new ProcessBuilder(fullCommand);
                    processBuilder.directory(workingDir.toFile());
                    processBuilder.redirectErrorStream(true);
                    Process process = processBuilder.start();
                    StringBuilder output = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            logger.info("Terraform output: {}", line);
                            output.append(line).append("\n");
                        }
                    }
                    int exitCode = process.waitFor(); // This could hang
                    String result = output.toString();
                    if (exitCode != 0) {
                        throw new IOException("Terraform command '" + command + "' failed with exit code " + exitCode + ": " + result);
                    }
                    return result;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(Duration.ofMinutes(5)) // Add a shorter timeout here
                .flatMap(output -> {
                    Mono<Void> stateHandling = command.equals("init")
                            ? Mono.empty()
                            : Mono.fromCallable(() -> Files.exists(workingDir.resolve(TF_STATE_FILE))
                                    ? Files.readString(workingDir.resolve(TF_STATE_FILE))
                                    : "")
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(state -> fileManager.writeFile(tenantId, TF_STATE_FILE, state));
                    return reactiveDatabaseService.logExecution(tenantId, templateId, userId, command,startedExecution,
                                    Instant.now(), output)
                            .then(stateHandling)
                            .thenReturn(output);
                })
                .onErrorMap(ex -> new RuntimeException("Terraform command execution failed: " + ex.getMessage(), ex));
    }

    private Mono<Void> acquireLock(String tenantId, UUID templateId, String userId) {
        String lockId = tenantId + "-" + templateId.toString();
        long currentTime = Instant.now().getEpochSecond();
        long ttl = currentTime + lockTtlSeconds;

        Map<String, AttributeValue> item = Map.of(
                "lockId", AttributeValue.builder().s(lockId).build(),
                "lockedAt", AttributeValue.builder().n(String.valueOf(currentTime)).build(),
                "ttl", AttributeValue.builder().n(String.valueOf(ttl)).build(),
                "owner", AttributeValue.builder().s(userId).build()
        );
        Map<String, String> expressionAttributeNames = Map.of("#ttl", "ttl");
        PutItemRequest request = PutItemRequest.builder()
                .tableName(LOCK_TABLE)
                .item(item)
                .conditionExpression("attribute_not_exists(lockId) OR #ttl < :currentTime")
                .expressionAttributeNames(expressionAttributeNames)
                .expressionAttributeValues(Map.of(
                        ":currentTime", AttributeValue.builder().n(String.valueOf(currentTime)).build()
                ))
                .build();
        return Mono.fromFuture(() -> dynamoDbClient.putItem(request))
                .retryWhen(reactor.util.retry.Retry.backoff(MAX_RETRY_ATTEMPTS, RETRY_DELAY)
                        .maxBackoff(MAX_BACKOFF)
                        .jitter(0.5)
                        .filter(throwable -> throwable instanceof ConditionalCheckFailedException)
                        .doBeforeRetry(signal -> logger.info("Retrying lock acquisition for {}", lockId)))
                .onErrorMap(ConditionalCheckFailedException.class,
                        ex -> new RuntimeException("Lock already acquired for template " + templateId, ex))
                .doOnSuccess(result -> logger.info("Acquired lock ID [{}]", lockId))
                .then();
    }

    private Mono<Void> releaseLock(String tenantId, UUID templateId) {
        String lockId = tenantId + "-" + templateId.toString();
        DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName(LOCK_TABLE)
                .key(Map.of("lockId", AttributeValue.builder().s(lockId).build()))
                .build();
        return Mono.fromFuture(() -> dynamoDbClient.deleteItem(request))
                .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofSeconds(1))
                        .doBeforeRetry(signal -> logger.warn("Retrying lock release for {}", lockId)))
                .doOnSuccess(result -> logger.info("Released lock ID [{}]", lockId))
                .onErrorMap(ex -> new RuntimeException("Failed to release lock for " + lockId + ": " + ex.getMessage(), ex))
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