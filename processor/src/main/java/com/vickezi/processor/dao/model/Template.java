package com.vickezi.processor.dao.model;

import java.util.UUID;

public record Template(UUID id, String tenantId, String name, String s3Key) {}