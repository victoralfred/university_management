package com.vickezi.messaging.queue.exception;

public class EmailSendingException extends RuntimeException{
    public EmailSendingException() {
    }

    public EmailSendingException(String message) {
        super(message);
    }

    public EmailSendingException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmailSendingException(Throwable cause) {
        super(cause);
    }
}
