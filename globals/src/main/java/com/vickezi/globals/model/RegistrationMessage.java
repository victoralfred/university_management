package com.vickezi.globals.model;

import com.vickezi.globals.events.Status;

import java.io.Serializable;

/**
 * Represents a stateless object for registering user email.
 * <p>
 * This object handles the registration process based on the state:
 * <ul>
 *   <li>{@linkplain Status#PENDING} - Treat it as a new registration.</li>
 *   <li>{@linkplain Status#IN_PROGRESS} - Treat it as a successful link, and create a user signup record with the email.</li>
 *   <li>{@linkplain Status#FAILED} - Clear all resources associated with the {@code messageId} so potential owners can restart the process.</li>
 * </ul>
 */

public record RegistrationMessage(String messageId, String token, String status, String email) implements Serializable {
    public RegistrationMessage(String messageId, String token, String status, String email) {
        this.messageId = messageId;
        this.token = token;
        this.status = Status.fromState(status).name();
        this.email = email;
    }

    @Override
    public String toString() {
        return "RegistrationMessage{" +
                "messageId='" + messageId + '\'' +
                ", token='" + token + '\'' +
                ", status=" + status +
                ", email='" + email + '\'' +
                '}';
    }
}
