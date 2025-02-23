package com.vickezi.processor.dao;

import com.vickezi.processor.dao.model.Template;
import com.vickezi.processor.model.InstanceProcessUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;
public final class ReactiveDatabaseService {
    private final Logger logger = LoggerFactory.getLogger(ReactiveDatabaseService.class);
    private final DatabaseClient dbClient;
    public ReactiveDatabaseService(DatabaseClient databaseClient) {
        this.dbClient = databaseClient;
    }
    public Mono<Template> fetchTemplateMetadata(String tenantId, UUID templateId) {
        return dbClient.sql("SELECT * FROM terraform_templates WHERE tenant_id = :tenantId AND id = :id")
                .bind("tenantId", tenantId)
                .bind("id", templateId)
                .map(row -> new Template(
                        row.get("id", UUID.class),
                        row.get("tenant_id", String.class),
                        row.get("name", String.class),
                        row.get("s3_key", String.class)
                ))
                .one().log();
    }
    public Mono<Void> logExecution(String tenantId, UUID templateId, String userId, String command, String output) {
        return dbClient.sql("INSERT INTO execution_history (id, template_id, tenant_id, executed_by, command, output, started_at, completed_at) " +
                        "VALUES (gen_random_uuid(), :templateId, :tenantId, :userId, :command, :output, :startedAt, :completedAt)")
                .bind("templateId", templateId)
                .bind("tenantId", tenantId)
                .bind("userId", userId)
                .bind("command", command)
                .bind("output", output)
                .bind("startedAt", Instant.now())
                .bind("completedAt", Instant.now())
                .then()
                .doOnSuccess(v -> logger.info("Logged execution for tenant {}, template {}", tenantId, templateId));
    }
    public Flux<Template> fetchAll() {
        return dbClient.sql("SELECT * FROM terraform_templates")
                .map(row -> new Template(
                        row.get("id", UUID.class),
                        row.get("tenant_id", String.class),
                        row.get("name", String.class),
                        row.get("s3_key", String.class)
                )).all().log();
    }
}
