package com.vickezi.processor.dao.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TenantRequest(@NotBlank(message = "Tenant ID must not be blank")
    @Size(min = 1, max = 50, message = "Tenant ID must be between 1 and 50 characters")String tenantId,
                            @NotBlank(message = "Tenant name must not be blank")
    @Size(min = 1, max = 100, message = "Tenant name must be between 1 and 100 characters")String name,
                            String config){
}
