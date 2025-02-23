package com.vickezi.processor.model;

import java.util.UUID;

public class InstanceProcessUsage {
    private UUID ID;
    private String instance;
    private int processUsage;

    public InstanceProcessUsage(String instance, int processUsage) {
        this.ID = UUID.randomUUID();
        this.instance = instance;
        this.processUsage = processUsage;
    }

    public String getInstance() {
        return instance;
    }

    public int getProcessUsage() {
        return processUsage;
    }

    public UUID getID() {
        return ID;
    }
}
