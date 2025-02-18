package com.vickezi.security.model;

import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import reactor.core.publisher.Mono;

public class CustomUserDetailsService implements ReactiveUserDetailsService {
    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return null;
    }
    private Mono<UserDetails> createUser(final Users users){
       return Mono.fromCallable(()-> new SecureUser(users));
    }
}
