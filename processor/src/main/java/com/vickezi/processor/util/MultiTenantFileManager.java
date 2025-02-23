package com.vickezi.processor.util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MultiTenantFileManager {
    private static final Logger logger = LoggerFactory.getLogger(MultiTenantFileManager.class);
    private final Path baseDirectory;
    private final ConcurrentHashMap<String, TenantFileContext> tenantContexts = new ConcurrentHashMap<>();
    private final Mono<Void> initialization;

    public MultiTenantFileManager(String baseDirectory) {
        this.baseDirectory = Path.of(baseDirectory);
        this.initialization = initializeBaseDirectory()
                .cache()
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<Void> initializeBaseDirectory() {
        return Mono.fromCallable(() -> {
                    if (!Files.exists(baseDirectory)) {
                        Files.createDirectories(baseDirectory);
                        logger.info("Initialized base directory: {}", baseDirectory);
                    }
                    return null;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
    private TenantFileContext getTenantContext(String tenantId) {
        return tenantContexts.computeIfAbsent(tenantId, id ->
                new TenantFileContext(baseDirectory.resolve(id)));
    }
    public Mono<Path> getTenantDirectory(String tenantId) {
        TenantFileContext context = getTenantContext(tenantId);
        return initialization
                .then(Mono.fromCallable(() -> {
                    if (!Files.exists(context.tenantBasePath)) {
                        Files.createDirectories(context.tenantBasePath);
                        logger.info("Created tenant directory: {}", context.tenantBasePath);
                    }
                    return context.tenantBasePath;
                }))
                .subscribeOn(Schedulers.boundedElastic());
    }
    public Mono<Void> writeFile(String tenantId, String fileName, String content) {
        TenantFileContext context = getTenantContext(tenantId);
        return Mono.fromCallable(() -> {
                    context.tenantLock.writeLock().lock();
                    try {
                        Path filePath = context.tenantBasePath.resolve(fileName);
                        // Directory already ensured by getTenantDirectory in the calling context
                        Files.writeString(filePath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                        logger.debug("Wrote file: {}", filePath);
                        return null;
                    } finally {
                        context.tenantLock.writeLock().unlock();
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
    public Mono<String> readFile(String tenantId, String fileName) {
        TenantFileContext context = getTenantContext(tenantId);
        return Mono.fromCallable(() -> {
                    context.tenantLock.readLock().lock();
                    try {
                        Path filePath = context.tenantBasePath.resolve(fileName);
                        return Files.readString(filePath);
                    } finally {
                        context.tenantLock.readLock().unlock();
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(IOException.class, e -> {
                    logger.error("Failed to read file {} for tenant {}: {}", fileName, tenantId, e.getMessage(), e);
                    return new RuntimeException("File read error: " + fileName, e);
                });
    }
    private static class TenantFileContext {
        private final Path tenantBasePath;
        private final ReadWriteLock tenantLock;

        TenantFileContext(Path tenantBasePath) {
            this.tenantBasePath = tenantBasePath;
            this.tenantLock = new ReentrantReadWriteLock();
        }
    }
}