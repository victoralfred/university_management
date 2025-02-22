package com.vickezi.processor.controller;

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


@RestController
@RequestMapping("/api/v1/process")
@CrossOrigin(origins = {"http://localhost:4200"}, methods = {RequestMethod.GET, RequestMethod.OPTIONS})
public class ProcessController {
    private final Logger logger = LoggerFactory.getLogger(ProcessController.class);
    private final MultiTenantTerraformService multiTenantTerraformService;
    private final Random random = new Random();
    public ProcessController(MultiTenantTerraformService multiTenantTerraformService) {
        this.multiTenantTerraformService = multiTenantTerraformService;
    }

    @PostMapping(path = "/enqueue/{tenantId}/{taskId}")
    public Mono<String> enqueueTask(@PathVariable String tenantId, @PathVariable String taskId){
        return multiTenantTerraformService.executeTerraformCommand(taskId, taskId);
    }
    @GetMapping(path = "/enqueue/{tenantId}/init")
    public Mono<String> executeTerraformCommand(@PathVariable String tenantId){
        return multiTenantTerraformService.executeTerraformCommand(tenantId, "init");
    }
    @GetMapping("/instance/process-usage")
    public Flux<InstanceProcessUsage> getProcessUsage() {
        logger.info("Request procesed for process state");
        return Flux.fromIterable(generateMockData());
//        Flux.interval(Duration.ofSeconds(5))
//                .map(tick ->generateMockData())
//                .flatMap(Flux::fromIterable);
    }

    private List<InstanceProcessUsage> generateMockData() {
        return List.of(
                new InstanceProcessUsage("Instance-1", random.nextInt(101)),
                new InstanceProcessUsage("Instance-2", random.nextInt(101)),
                new InstanceProcessUsage("Instance-3", random.nextInt(101))
        );
    }
}
