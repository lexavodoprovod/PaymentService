package com.innowise.paymentservice.controller;

import com.innowise.paymentservice.dto.kafkadto.PaymentEventDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class KafkaMessageController {

    private final KafkaTemplate<String, PaymentEventDto> kafkaTemplate;

    @Value("${topic-name.status}")
    private String statusTopicName;

    @PostMapping
    public void publishStatusEvent(@RequestBody PaymentEventDto paymentEventDto) {
        kafkaTemplate.send(statusTopicName, paymentEventDto);
    }
}
