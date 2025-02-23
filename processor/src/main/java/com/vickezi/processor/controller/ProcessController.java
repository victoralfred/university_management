package com.vickezi.processor.controller;

import com.vickezi.processor.dao.ReactiveDatabaseService;
import com.vickezi.processor.dao.model.TenantRequest;
import com.vickezi.processor.dao.model.TenantService;
import com.vickezi.processor.executions.MultiTenantTerraformService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;


@RestController
@RequestMapping("/api/v1/process")
@CrossOrigin(origins = {"http://localhost:4200"}, methods = {RequestMethod.GET, RequestMethod.OPTIONS})
public class ProcessController {
    private final Logger logger = LoggerFactory.getLogger(ProcessController.class);
    private final MultiTenantTerraformService multiTenantTerraformService;
    private final ReactiveDatabaseService reactiveDatabaseService;
    private final TenantService tenantService;
    public ProcessController(MultiTenantTerraformService multiTenantTerraformService,
                             ReactiveDatabaseService reactiveDatabaseService, TenantService tenantService) {
        this.multiTenantTerraformService = multiTenantTerraformService;
        this.reactiveDatabaseService = reactiveDatabaseService;
        this.tenantService = tenantService;
    }
    @PostMapping("/tenants/{tenantId}/templates/{templateId}/execute")
    public Mono<ResponseEntity<ExecutionResponse>> executeTerraform(
            @PathVariable String tenantId,
            @PathVariable UUID templateId,
            @RequestBody TerraformRequest request,
            @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserIdFromToken(authHeader); // Assume a utility method
        return multiTenantTerraformService.executeTerraformCommand(tenantId, templateId, userId, request.command(), request.args())
                .map(output -> {
                    ExecutionResponse response = new ExecutionResponse("success", output, UUID.randomUUID().toString());
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(IllegalArgumentException.class, ex ->
                        Mono.just(ResponseEntity.badRequest().body(new ExecutionResponse("error", ex.getMessage(), null))))
                .onErrorResume(RuntimeException.class, ex -> {
                    if (ex.getMessage().contains("Lock already acquired")) {
                        logger.error("Lock already acquired", ex);
                        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(new ExecutionResponse("error", ex.getMessage(), null)));
                    }
                    logger.error("Failed to add tenant", ex);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(new ExecutionResponse("error", ex.getMessage(), null)));
                });
    }
    @PostMapping
    public Mono<ResponseEntity<TenantResponse>> createTenant(
            @Valid @RequestBody TenantRequest tenantRequest,
            @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserIdFromToken(authHeader); // Assume this validates the user

        TenantRequest tenant = new TenantRequest(tenantRequest.tenantId(),
                tenantRequest.name(), tenantRequest.config());
        return tenantService.saveTenant(tenant)
                .map(savedTenant -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(new TenantResponse("success", "Tenant created", savedTenant)))
                .onErrorResume(IllegalArgumentException.class, ex ->
                        Mono.just(ResponseEntity.badRequest()
                                .body(new TenantResponse("error", ex.getMessage(), null))))
                .onErrorResume(IllegalStateException.class, ex ->
                        Mono.just(ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(new TenantResponse("error", ex.getMessage(), null))))
                .onErrorResume(Exception.class, ex ->
                        Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(new TenantResponse("error", "Failed to create tenant: " + ex.getMessage(), null))));
    }
    private String extractUserIdFromToken(String authHeader) {
        // Placeholder: Implement JWT/OAuth2 token parsing
        return "user123";
    }

    // Example DTOs
    public record TerraformRequest(String command, String[] args){}

    /**
     * @param output or 'message' for errors
     */
    public record ExecutionResponse(String status, String output, String executionId) {
    }
    public record TenantResponse(String status, String message, TenantRequest tenantRequest){}
}
