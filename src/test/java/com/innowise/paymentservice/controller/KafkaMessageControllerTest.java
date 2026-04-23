package com.innowise.paymentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.paymentservice.dto.kafkadto.PaymentEventDto;
import com.innowise.paymentservice.entity.Status;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KafkaMessageControllerTest extends BaseIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${topic-name.status}")
    private String topicName;


    private static final String KAFKA_API_PATH = "/api/v1/messages";

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName
            .parse("confluentinc/cp-kafka:7.5.0")
            .asCompatibleSubstituteFor("apache/kafka"))
            .withKraft()
            .withStartupTimeout(Duration.ofMinutes(3));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Nested
    @DisplayName("Kafka Message Integration Tests")
    class KafkaIntegrationTests {

        @Test
        @DisplayName("Should publish payment event to Kafka topic")
        void shouldPublishEventToKafka() throws Exception {
            PaymentEventDto eventDto = new PaymentEventDto(123L, Status.SUCCESS);

            Consumer<String, PaymentEventDto> consumer = createTestConsumer();
            consumer.subscribe(Collections.singleton(topicName));

            mockMvc.perform(MockMvcRequestBuilders.post(KAFKA_API_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(eventDto)))
                    .andExpect(MockMvcResultMatchers.status().isOk());

            ConsumerRecord<String, PaymentEventDto> receivedRecord =
                    KafkaTestUtils.getSingleRecord(consumer, topicName, Duration.ofSeconds(5));

            assertNotNull(receivedRecord);
            assertEquals(eventDto.orderId(), receivedRecord.value().orderId());
            assertEquals(eventDto.status(), receivedRecord.value().status());

            consumer.close();
        }

        @Test
        @DisplayName("Should return 400 when message body is invalid")
        void shouldReturnBadRequestForInvalidBody() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(KAFKA_API_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"orderId\": \"not-a-number\"}"))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }
    }


    private Consumer<String, PaymentEventDto> createTestConsumer() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                kafka.getBootstrapServers(),
                "test-group",
                "true"
        );

        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        JsonDeserializer<PaymentEventDto> jsonDeserializer = new JsonDeserializer<>(PaymentEventDto.class);
        jsonDeserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(
                consumerProps,
                new StringDeserializer(),
                jsonDeserializer
        ).createConsumer();
    }
}