package com.vickezi.globals.events;

/**
 * Represents the current state of a process
 */
public enum Status {
    PENDING("pending"),
    IN_PROGRESS("in_progress"),
    COMPLETED("completed"),
    FAILED("failed"),
    TO_RETRY("to_retry"),
    SUCCEEDED("succeeded");
    private final String state;
    Status(String state) {
        this.state = state;
    }
    public String getState() {
        return state;
    }
    public static Status fromState(String state) {
        for (Status status : Status.values()) {
            if (status.getState().equalsIgnoreCase(state)) {
                return status;
            }
        }
        throw new IllegalArgumentException(String.format("This is an unknown state: %s", state));
    }
}
