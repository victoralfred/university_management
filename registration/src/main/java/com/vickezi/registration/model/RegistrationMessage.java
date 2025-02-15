package com.vickezi.registration.model;

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

public class RegistrationMessage implements Serializable {
    private  final String messageId;
    private final String token;
    private final Status status;
    private final String email;

    public RegistrationMessage(String messageId, String token, String status, String email) {
        this.messageId = messageId;
        this.token = token;
        this.status = Status.fromState(status);
        this.email = email;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getToken() {
        return token;
    }

    public String getEmail() {
        return email;
    }

    public Status getStatus() {
        return status;
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
