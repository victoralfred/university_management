package com.vickezi.messaging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MessagingService {
    public static void main(String[] args) {
        SpringApplication.run(MessagingService.class, args);
    }
}
