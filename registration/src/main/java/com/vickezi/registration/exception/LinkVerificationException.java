package com.vickezi.registration.exception;

public class LinkVerificationException extends RuntimeException{
    public LinkVerificationException() {
    }

    public LinkVerificationException(String message) {
        super(message);
    }

    public LinkVerificationException(String message, Throwable cause) {
        super(message, cause);
    }

    public LinkVerificationException(Throwable cause) {
        super(cause);
    }

    public LinkVerificationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
