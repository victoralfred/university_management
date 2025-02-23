package com.vickezi.processor.executions;

import com.vickezi.processor.dao.ReactiveDatabaseService;
import com.vickezi.processor.dao.model.Template;
import com.vickezi.processor.util.MultiTenantFileManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import java.util.concurrent.CompletableFuture;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static reactor.core.publisher.Flux.just;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@ExtendWith(MultiTenantTerraformServiceTest.TestResultLoggerExtension.class)
public class MultiTenantTerraformServiceTest {
    private static final Logger logger = LoggerFactory.getLogger(MultiTenantTerraformServiceTest.class);
    private static final Path TEST_RESULT_FILE = Paths.get("test-results.txt");
    private static final String TENANT_ID = "15003103";
    private static final UUID TEMPLATE_ID = UUID.randomUUID();
    private static final String USER_ID = "user1";
    private static final String BASE_DIR = "C:\\project\\university-portal\\processor\\target\\test-terraform";

    @Mock private S3AsyncClient s3Client;
    @Mock private DynamoDbAsyncClient dynamoDbClient;
    @Mock private DatabaseClient dbClient;
    @Mock private ReactiveDatabaseService databaseService;
    @Autowired
    private MultiTenantTerraformService terraformService;


    @AfterAll
    static void cleanup() {
        // Cleanup test directory
        try {
            Files.walk(Path.of(BASE_DIR))
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            logger.warn("Failed to delete {}", p, e);
                        }
                    });
        } catch (IOException e) {
            logger.error("Cleanup failed", e);
        }
    }

    @Test
    void testExecuteTerraformCommandInitSuccess() {
        // Arrange
        DatabaseClient.GenericExecuteSpec executeSpec = mock(DatabaseClient.GenericExecuteSpec.class);
        when(databaseService.fetchTemplateMetadata(TENANT_ID, TEMPLATE_ID))
                .thenReturn(Mono.just(new com.vickezi.processor.dao.model.Template(UUID.fromString("02ebe298-1037-4fbf-88dd-a3115930aeed"),
                        TENANT_ID,"jemimah",
                        "s3://test/main.tf")));
        when(s3Client.getObject((GetObjectRequest) any(GetObjectRequest.class), (AsyncResponseTransformer<GetObjectResponse, Object>) any()))
                .thenReturn(CompletableFuture.completedFuture(GetObjectResponse.builder().build()));
        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(PutItemResponse.builder().build()));
        when(dynamoDbClient.deleteItem(any(DeleteItemRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(DeleteItemResponse.builder().build()));
        when(dbClient.sql(any(String.class))).thenReturn(executeSpec);
        when(executeSpec.bind(any(String.class), any())).thenReturn(executeSpec);
        when(executeSpec.then()).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(terraformService.executeTerraformCommand(TENANT_ID, TEMPLATE_ID, USER_ID, "init"))
                .expectNextMatches(output -> output.contains("Terraform has been successfully initialized"))
                .verifyComplete();
    }



    @Test
    void testExecuteTerraformCommandLockFailure() {
        // Arrange
        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Lock Exists")));
        when(dynamoDbClient.deleteItem(any(DeleteItemRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(DeleteItemResponse.builder().build()));

        // Act & Assert
        StepVerifier.create(terraformService.executeTerraformCommand(TENANT_ID, TEMPLATE_ID, USER_ID, "init"))
                .expectErrorMatches(t -> t instanceof RuntimeException && t.getMessage().contains("Terraform execution failed for tenant "+TENANT_ID))
                .verify();
    }

    @Test
    void testExecuteTerraformCommandS3Failure() {
        DatabaseClient.GenericExecuteSpec executeSpec = mock(DatabaseClient.GenericExecuteSpec.class);
        when(databaseService.fetchTemplateMetadata(TENANT_ID, TEMPLATE_ID))
                .thenReturn(Mono.just(new com.vickezi.processor.dao.model.Template(UUID.fromString("02ebe298-1037-4fbf-88dd-a3115930aeed"),
                        TENANT_ID,"jemimah",
                        "s3://test/main.tf")));
        when(s3Client.getObject((GetObjectRequest) any(GetObjectRequest.class), (AsyncResponseTransformer<GetObjectResponse, Object>) any()))
                .thenReturn(CompletableFuture.failedFuture(new IOException("S3 error")));
        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(PutItemResponse.builder().build()));
        when(dynamoDbClient.deleteItem(any(DeleteItemRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(DeleteItemResponse.builder().build()));
        when(dbClient.sql(any(String.class))).thenReturn((DatabaseClient.GenericExecuteSpec) dbClient);
        when(((DatabaseClient.GenericExecuteSpec) dbClient).bind(any(String.class), any())).thenReturn((DatabaseClient.GenericExecuteSpec) dbClient);
        when(((DatabaseClient.GenericExecuteSpec) dbClient).then()).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(terraformService.executeTerraformCommand(TENANT_ID, TEMPLATE_ID, USER_ID, "init"))
                .expectNextMatches(output -> output.contains("provider \"null\" {}"))
                .verifyComplete();
    }
    record Template(String s3Key) {}
    @Test
    void testGetTenantDirectorySuccess() {
        MultiTenantFileManager fileManager = new MultiTenantFileManager(BASE_DIR);
        StepVerifier.create(fileManager.getTenantDirectory(TENANT_ID))
                .expectNextMatches(path -> path.endsWith(Path.of(TENANT_ID)))
                .verifyComplete();
    }

    @Test
    void testWriteFileSuccess() {
        MultiTenantFileManager fileManager = new MultiTenantFileManager(BASE_DIR);
        String fileName = "test.tf";
        String content = "test content";

        StepVerifier.create(fileManager.getTenantDirectory(TENANT_ID)
                        .then(fileManager.writeFile(TENANT_ID, fileName, content))
                        .then(fileManager.readFile(TENANT_ID, fileName)))
                .expectNext(content)
                .verifyComplete();
    }

    @Test
    void testReadFileFailure() {
        MultiTenantFileManager fileManager = new MultiTenantFileManager(BASE_DIR);
        StepVerifier.create(fileManager.readFile(TENANT_ID, "nonexistent.tf"))
                .expectErrorMatches(t -> t instanceof RuntimeException && t.getMessage().contains("File read error"))
                .verify();
    }



    // Custom extension to log test results
    static class TestResultLoggerExtension implements TestWatcher {
        @Override
        public void testDisabled(ExtensionContext context, Optional<String> reason) {
            TestWatcher.super.testDisabled(context, reason);
        }

        @Override
        public void testSuccessful(ExtensionContext context) {
            TestWatcher.super.testSuccessful(context);
            logResult(context.getDisplayName(), "PASSED");
        }

        @Override
        public void testAborted(ExtensionContext context, Throwable cause) {
            TestWatcher.super.testAborted(context, cause);
            logResult(context.getDisplayName(), "ABORTED: " + cause.getMessage());
        }

        @Override
        public void testFailed(ExtensionContext context, Throwable cause) {
            TestWatcher.super.testFailed(context, cause);
            logResult(context.getDisplayName(), "FAILED: " + cause.getMessage());
        }


        private void logResult(String testInfo, String status) {
            String result = String.format("%s: %s\n", testInfo, status);
            try {
                Files.writeString(TEST_RESULT_FILE, result, StandardOpenOption.APPEND);
            } catch (IOException e) {
                logger.error("Failed to write test result", e);
            }
        }
    }
}
