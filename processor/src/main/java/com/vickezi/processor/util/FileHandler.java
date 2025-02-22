package com.vickezi.processor.util;

import com.vickezi.processor.executions.MultiTenantTerraformService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileHandler {
    private final Logger logger = LoggerFactory.getLogger(FileHandler.class);
    private static final String TERRAFORM_FILENAME = "terraform.tf";
    private static final ConcurrentHashMap<String, Object> lockMap = new ConcurrentHashMap<>();
    private final String tenantsTerraformPath;
    private final String tenantId;
    private String terraformConfigPath;

    public FileHandler(String tenantsTerraformPath, String tenantId) {
        this.tenantsTerraformPath = tenantsTerraformPath;
        this.tenantId = tenantId;
    }
    public Path getTerraformConfig() throws IOException {
        return ensureTenantDirectory();
    }
    private Path ensureTenantDirectory() throws IOException {
        Path tenantDir = Paths.get(tenantsTerraformPath, tenantId);
        if (!Files.exists(tenantDir)) {
            Files.createDirectories(tenantDir);
            logger.info("Created tenant directory: [{}]", tenantDir);
        }
        return tenantDir;
    }
    public String getDefaultTerraformConfig() {
        return """
            terraform {
              required_version = ">= 1.0.0"
            }
            """;
    }


}
