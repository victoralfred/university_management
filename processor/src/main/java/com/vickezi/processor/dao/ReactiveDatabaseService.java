package com.vickezi.processor.dao;

import com.vickezi.processor.dao.model.Template;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;
import java.util.UUID;
public final class ReactiveDatabaseService {
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
}
