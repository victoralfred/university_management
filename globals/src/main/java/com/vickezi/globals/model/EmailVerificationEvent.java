package com.vickezi.globals.model;

public record EmailVerificationEvent (String token, String messageId){
}
