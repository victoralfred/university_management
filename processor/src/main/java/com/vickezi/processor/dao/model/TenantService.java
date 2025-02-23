package com.vickezi.processor.dao.model;

import com.vickezi.processor.dao.ReactiveDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class TenantService {
    private static final Logger logger = LoggerFactory.getLogger(TenantService.class);
    private final ReactiveDatabaseService reactiveDatabaseService;
    public TenantService(ReactiveDatabaseService reactiveDatabaseService) {
        this.reactiveDatabaseService = reactiveDatabaseService;
    }
    public Mono<TenantRequest> saveTenant(TenantRequest tenantRequest) {
        // Additional business logic/validation
        if (tenantRequest.tenantId().contains("..") || tenantRequest.tenantId().contains("/")) {
            return Mono.error(new IllegalArgumentException("Tenant ID must not contain path separators"));
        }
        return reactiveDatabaseService.checkTenantExists(tenantRequest.tenantId())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new IllegalStateException("Tenant with ID " + tenantRequest.tenantId() + " already exists"));
                    }
                    return reactiveDatabaseService.saveTenant(tenantRequest)
                            .doOnSuccess(savedTenantRequest -> logger.info("Saved tenant: {}", savedTenantRequest.tenantId()));
                })
                .onErrorMap(ex -> {
                    logger.error("Failed to save tenant {}: {}", tenantRequest.tenantId(), ex.getMessage());
                    return new RuntimeException("Failed to save tenant: " + ex.getMessage(), ex);
                });
    }
}