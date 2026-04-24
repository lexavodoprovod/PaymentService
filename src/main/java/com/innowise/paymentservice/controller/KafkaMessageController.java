package com.innowise.paymentservice.controller;

import com.innowise.paymentservice.dto.kafkadto.PaymentEventDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller responsible for manual triggering of Kafka events.
 * Primarily used for testing or administrative purposes to publish payment status updates
 * to the message broker.
 */
@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class KafkaMessageController {

    /**
     * Template for performing high-level Kafka operations, configured to send
     * {@link PaymentEventDto} objects.
     */
    private final KafkaTemplate<String, PaymentEventDto> kafkaTemplate;

    /**
     * The name of the Kafka topic where payment status events are published.
     * Value is injected from application properties.
     */
    @Value("${topic-name.status}")
    private String statusTopicName;

    /**
     * Publishes a payment status event to the configured Kafka topic.
     * * @param paymentEventDto the data transfer object containing payment status details
     */
    @PostMapping
    public void publishStatusEvent(@RequestBody PaymentEventDto paymentEventDto) {
        kafkaTemplate.send(statusTopicName, paymentEventDto);
    }
}