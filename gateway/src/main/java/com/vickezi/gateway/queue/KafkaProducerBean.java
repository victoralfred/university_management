package com.vickezi.gateway.queue;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;


import static com.vickezi.globals.util.Constants.USER_EMAIL_REGISTERED_EVENT_TOPIC;
import static com.vickezi.globals.util.Constants.USER_REGISTRATION_CONFIRMATION_NOTICE_TOPIC;

@Configuration
public class KafkaProducerBean {
    @Bean
    public NewTopic newUserEmailRegisteredTopic(){
        return TopicBuilder.name(USER_EMAIL_REGISTERED_EVENT_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic newUserEmailConfirmationTopic(){
        return TopicBuilder.name(USER_REGISTRATION_CONFIRMATION_NOTICE_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

}
