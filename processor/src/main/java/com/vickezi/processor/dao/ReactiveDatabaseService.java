package com.vickezi.processor.dao;

import com.vickezi.processor.dao.model.TemplateMetadata;
import com.vickezi.processor.dao.model.TenantRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;
public interface ReactiveDatabaseService {
    // Tenant-related methods
    Mono<TenantRequest> saveTenant(TenantRequest tenantRequest);
    Mono<Boolean> checkTenantExists(String tenantId);
    Mono<TenantRequest> getTenant(String tenantId);

    // Template metadata method (from MultiTenantTerraformService)
    Mono<TemplateMetadata> fetchTemplateMetadata(String tenantId, UUID templateId);

    // Execution logging method (from MultiTenantTerraformService)
    Mono<Void> logExecution(String tenantId, UUID templateId, String userId, String command, Instant started, Instant completed, String output);
    Flux<TemplateMetadata> fetchAll();
}
