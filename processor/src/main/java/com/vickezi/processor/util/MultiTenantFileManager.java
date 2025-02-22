package com.vickezi.processor.util;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MultiTenantFileManager {
    private final Path baseDirectory;
    private final Map<String, ReadWriteLock> tenantLocks = new ConcurrentHashMap<>();
    private final Mono<Void> initialization;

    public MultiTenantFileManager(String baseDirectory) {
        this.baseDirectory = Path.of(baseDirectory);
        this.initialization = Mono.fromRunnable(() -> {
                    try {
                        if (!Files.exists(this.baseDirectory)) {
                            Files.createDirectories(this.baseDirectory);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to initialize base directory: " + this.baseDirectory, e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .cache().then();
    }

    public Mono<Path> getTenantDirectory(String tenantId) {
        Path tenantDir = baseDirectory.resolve(tenantId);
        return initialization
                .then(Mono.fromRunnable(() -> {
                    try {
                        if (!Files.exists(tenantDir)) {
                            Files.createDirectories(tenantDir);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to create tenant directory: " + tenantDir, e);
                    }
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .then(Mono.just(tenantDir));
    }

    public Mono<Void> writeFile(String tenantId, String fileName, String content) {
        ReadWriteLock lock = tenantLocks.computeIfAbsent(tenantId, k -> new ReentrantReadWriteLock());
        return initialization
                .then(Mono.fromRunnable(() -> {
                    lock.writeLock().lock();
                    try {
                        Path tenantDir = baseDirectory.resolve(tenantId);
                        Files.writeString(tenantDir.resolve(fileName), content);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to write file: " + fileName, e);
                    } finally {
                        lock.writeLock().unlock();
                    }
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    public Mono<String> readFile(String tenantId, String fileName) {
        ReadWriteLock lock = tenantLocks.computeIfAbsent(tenantId, k -> new ReentrantReadWriteLock());
        return initialization
                .then(Mono.fromCallable(() -> {
                    lock.readLock().lock();
                    try {
                        Path tenantDir = baseDirectory.resolve(tenantId);
                        return Files.readString(tenantDir.resolve(fileName));
                    } finally {
                        lock.readLock().unlock();
                    }
                }))
                .subscribeOn(Schedulers.boundedElastic());
    }

    // New method to initialize tenant with a default configuration
    public Mono<Void> initializeTenantConfig(String tenantId, String configContent) {
        return getTenantDirectory(tenantId)
                .flatMap(tenantDir -> {
                    Path configFile = tenantDir.resolve("main.tf");
                    return Mono.fromCallable(() -> Files.exists(configFile))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(exists -> {
                                if (!exists) {
                                    return writeFile(tenantId, "main.tf", configContent);
                                }
                                return Mono.empty();
                            });
                });
    }
}