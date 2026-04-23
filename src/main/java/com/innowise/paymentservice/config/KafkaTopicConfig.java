package com.innowise.paymentservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${topic-name.status}")
    private String statusTopicName;

    @Bean
    public NewTopic statusTopic(){
        return TopicBuilder.name(statusTopicName)
                .build();
    }
}
