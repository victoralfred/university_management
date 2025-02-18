package com.vickezi.gateway.queue.bean;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import static com.vickezi.globals.util.Constants.EMAIL_VERIFICATION_MESSAGE_TOPIC;
import static com.vickezi.globals.util.Constants.USER_EMAIL_REGISTERED_EVENT_TOPIC;


@Configuration
public class KafKaTopicManager {
    @Bean
    public NewTopic createServiceTopic() {
        return TopicBuilder.name(USER_EMAIL_REGISTERED_EVENT_TOPIC)
                        .partitions(1)
                        .replicas(1)
                        .build();
    }
    @Bean
    public NewTopic emailVerificationTopic(){
        return TopicBuilder.name(EMAIL_VERIFICATION_MESSAGE_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
