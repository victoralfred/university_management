package com.vickezi.registration.exception;

public class SignatureVerificationException  extends RuntimeException{
    public SignatureVerificationException(Throwable cause) {
        super(cause);
    }

    public SignatureVerificationException(String message, Throwable cause) {
        super(message, cause);
    }

    public SignatureVerificationException(String message) {
        super(message);
    }
}
