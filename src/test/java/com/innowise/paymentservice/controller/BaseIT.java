package com.innowise.paymentservice.controller;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.innowise.paymentservice.dto.kafkadto.PaymentEventDto;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;


@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class BaseIT {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName
            .parse("confluentinc/cp-kafka:7.5.0"))
            .withStartupTimeout(Duration.ofMinutes(3));

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().port(8090))
            .build();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("random-number-api.url", wireMock::baseUrl);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

//    protected Consumer<String, PaymentEventDto> createTestConsumer() {
//        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
//                kafka.getBootstrapServers(),
//                "test-group",
//                "true"
//        );
//
//        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
//
//        JsonDeserializer<PaymentEventDto> jsonDeserializer = new JsonDeserializer<>(PaymentEventDto.class);
//        jsonDeserializer.addTrustedPackages("*");
//
//        return new DefaultKafkaConsumerFactory<>(
//                consumerProps,
//                new StringDeserializer(),
//                jsonDeserializer
//        ).createConsumer();
//    }
}
