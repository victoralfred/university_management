package com.vickezi.processor.executions;
import com.vickezi.processor.util.MultiTenantFileManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.Duration;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class MultiTenantTerraformService {
    @Value("${terraform.base.directory:/tmp/terraform}") String baseDirectory;
    @Value("${terraform.binary.path:/usr/local/bin/terraform}") String terraformBinaryPath;
    @Value("${terraform.default.config:provider \"null\" {}}") String defaultConfig;
    @Value("${terraform.default.config:provider \"null\" {}}") String s3BucketBase;

    public Mono<String> executeTerraformCommand(String tenantId, String command, String... args) {
        final MultiTenantFileManager fileManager = new MultiTenantFileManager(baseDirectory);
        return fileManager.initializeTenantConfig(tenantId, defaultConfig) // Ensure config exists
                .then(fileManager.getTenantDirectory(tenantId))
                .flatMap(tenantDir -> runTerraformCommand(tenantId, tenantDir, command, args))
                .timeout(Duration.ofMinutes(30))
                .onErrorMap(ex -> new RuntimeException("Terraform execution failed for tenant " + tenantId, ex));
    }

    private Mono<String> runTerraformCommand(String tenantId, Path workingDir, String command, String[] args) {
        final MultiTenantFileManager fileManager = new MultiTenantFileManager(baseDirectory);
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
                    String outputFileName = String.format("terraform_%s_%d.log", command, System.currentTimeMillis());
                    return fileManager.writeFile(tenantId, outputFileName, output).thenReturn(output);
                });
    }

    private String[] buildCommandArray(String command, String[] args) {
        String[] baseCommand = {terraformBinaryPath, command};
        String[] result = new String[baseCommand.length + args.length];
        System.arraycopy(baseCommand, 0, result, 0, baseCommand.length);
        System.arraycopy(args, 0, result, baseCommand.length, args.length);
        return result;
    }
}