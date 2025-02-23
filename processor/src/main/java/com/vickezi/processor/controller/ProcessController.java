package com.vickezi.processor.controller;

import com.vickezi.processor.dao.ReactiveDatabaseService;
import com.vickezi.processor.dao.model.Template;
import com.vickezi.processor.executions.MultiTenantTerraformService;
import com.vickezi.processor.model.InstanceProcessUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.UUID;


@RestController
@RequestMapping("/api/v1/process")
@CrossOrigin(origins = {"http://localhost:4200"}, methods = {RequestMethod.GET, RequestMethod.OPTIONS})
public class ProcessController {
    private final Logger logger = LoggerFactory.getLogger(ProcessController.class);
    private final MultiTenantTerraformService multiTenantTerraformService;
    private final ReactiveDatabaseService reactiveDatabaseService;
    private final Random random = new Random();
    public ProcessController(MultiTenantTerraformService multiTenantTerraformService, ReactiveDatabaseService reactiveDatabaseService) {
        this.multiTenantTerraformService = multiTenantTerraformService;
        this.reactiveDatabaseService = reactiveDatabaseService;
    }

    @PostMapping(path = "/enqueue/{tenantId}/{taskId}")
    public Mono<String> enqueueTask(@PathVariable String tenantId, @PathVariable String taskId){
        return null;
    }
    @GetMapping(path = "/enqueue/{tenantId}/{command}")
    public Mono<String> executeTerraformCommand(@PathVariable String tenantId, @PathVariable String command){
        String tenantIds = (String) "auth.getDetails()"; // Assume tenantId from JWT
        String userId = "jemimah";
        return multiTenantTerraformService.executeTerraformCommand(tenantId,
                UUID.fromString("02ebe298-1037-4fbf-88dd-a3115930aeed"),
                userId,command);
    }
    @GetMapping("/templates")
    public Flux<Template> getProcessUsage() {
        return reactiveDatabaseService.fetchAll();

    }

    private List<InstanceProcessUsage> generateMockData() {
        return List.of(
                new InstanceProcessUsage("Instance-1", random.nextInt(101)),
                new InstanceProcessUsage("Instance-2", random.nextInt(101)),
                new InstanceProcessUsage("Instance-3", random.nextInt(101))
        );
    }
}
