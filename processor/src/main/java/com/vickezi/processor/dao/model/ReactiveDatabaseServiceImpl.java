package com.vickezi.processor.dao.model;

import com.vickezi.processor.dao.ReactiveDatabaseService;

import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

public class ReactiveDatabaseServiceImpl implements ReactiveDatabaseService {
    private final DatabaseClient databaseClient;
    public ReactiveDatabaseServiceImpl(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }
    @Override
    public Mono<TenantRequest> saveTenant(TenantRequest tenantRequest) {
        // Additional business logic/validation
        if (tenantRequest.tenantId().contains("..") || tenantRequest.tenantId().contains("/")) {
            return Mono.error(new IllegalArgumentException("Tenant ID must not contain path separators"));
        }
        return databaseClient.sql(
                        "INSERT INTO tenants (tenant_id, name, config) VALUES (:tenantId, :name, :config) " +
                                "ON CONFLICT (tenant_id) DO UPDATE SET name = :name, config = :config " +
                                "RETURNING tenant_id, name, config")
                .bind("tenant_Id", tenantRequest.tenantId())
                .bind("name", tenantRequest.name())
                .bind("config", tenantRequest.config() != null ? tenantRequest.config() : "{}") // Default to empty JSON
                .map(row -> new TenantRequest(
                        row.get("tenantId", String.class),
                        row.get("name", String.class),
                        row.get("config", String.class)))
                .one();
    }
    @Override
    public Mono<Boolean> checkTenantExists(String tenantId) {
        return databaseClient.sql("SELECT COUNT(*) FROM tenants WHERE tenant_id = :tenantId")
                .bind("tenant_Id", tenantId)
                .map(row -> row.get(0, Long.class) > 0)
                .one();
    }
    @Override
    public Mono<TenantRequest> getTenant(String tenantId) {
        return databaseClient.sql("SELECT tenant_id, name, config FROM tenants WHERE tenant_id = :tenantId")
                .bind("tenant_Id", tenantId)
                .map(row -> new TenantRequest(
                        row.get("tenant_id", String.class),
                        row.get("name", String.class),
                        row.get("config", String.class)))
                .one()
                .switchIfEmpty(Mono.empty()); // Return empty Mono if tenant not found
    }
    @Override
    public Mono<TemplateMetadata> fetchTemplateMetadata(String tenantId, UUID templateId) {
        if (tenantId.contains("..") || tenantId.contains("/")) {
            return Mono.error(new IllegalArgumentException("Tenant ID must not contain path separators"));
        }
        return databaseClient.sql(
                        "SELECT * FROM template_metadata " +
                                "WHERE tenant_id = :tenantId AND template_id = :templateId")
                .bind("tenantId", tenantId)
                .bind("templateId", templateId)
                .map(row -> new TemplateMetadata(
                        row.get("template_id", UUID.class),
                        row.get("tenant_id", String.class),
                        row.get("template_id", UUID.class),
                        row.get("name", String.class),
                        row.get("s3_key", String.class)))
                .one()
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Template " + templateId + " not found for tenant " + tenantId)));
    }
    @Override
    public Mono<Void> logExecution(String tenantId, UUID templateId, String executedBy, String command,
                                   Instant startedAt, Instant completedAt, String output) {
        UUID executionId = UUID.randomUUID();
        return databaseClient.sql(
                        "INSERT INTO executions (execution_id, tenant_id, template_id, executed_by, command, output, started_at, completed_at) " +
                                "VALUES (:executionId, :tenantId, :templateId, :executedBy, :command, :output, :startedAt,:completedAt)")
                .bind("executionId", executionId)
                .bind("tenantId", tenantId)
                .bind("templateId", templateId)
                .bind("executedBy", executedBy)
                .bind("command", command)
                .bind("output", output)
                .bind("startedAt", startedAt)
                .bind("completedAt",completedAt)
                .then();
    }
    public Flux<TemplateMetadata> fetchAll() {
        return databaseClient.sql("SELECT * FROM terraform_templates")
                .map(row -> new TemplateMetadata(
                        row.get("id", UUID.class),
                        row.get("tenant_id", String.class),
                        row.get("template_id", UUID.class),
                        row.get("user", String.class),
                        row.get("s3_key", String.class)
                )).all().log();
    }
}
