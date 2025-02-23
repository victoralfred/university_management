package com.vickezi.processor.dao.model;

import java.util.UUID;

public record TemplateMetadata(UUID id, String tenantId, UUID templateId, String name, String s3Key) {

}