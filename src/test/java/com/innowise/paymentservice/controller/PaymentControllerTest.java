package com.innowise.paymentservice.controller;

import com.innowise.paymentservice.dto.kafkadto.PaymentEventDto;
import com.innowise.paymentservice.dto.request.PaymentRequestDto;
import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.entity.Status;
import com.innowise.paymentservice.repository.PaymentRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class PaymentControllerTest extends BaseIT{

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    private static String topicName = "status-topic";

    private static Consumer<String, PaymentEventDto> consumer;

    private static final String CONTROLLER_PATH = "/api/v1/payments";


    @BeforeAll
    static void beforeAll() {
        consumer = createTestConsumer();
        consumer.subscribe(Collections.singleton(topicName));
    }

    @AfterAll
    static void afterAll() {
        if(consumer != null) {
            consumer.close();
        }
    }

    @BeforeEach
    void beforeEach() {
        mongoTemplate.getCollectionNames().forEach(coll -> mongoTemplate.dropCollection(coll));

        consumer.poll(Duration.ofMillis(100));
        Set<TopicPartition> assignment = consumer.assignment();
        consumer.seekToEnd(assignment);
        assignment.forEach(tp -> consumer.position(tp));
    }

    @Nested
    @DisplayName("Add Payment Integration Tests")
    class AddPaymentIntegrationTests {

        @Test
        @DisplayName("Should create payment and publish event with SUCCESS to Kafka")
        void shouldCreatePaymentAndSendKafkaEvent() throws Exception {
            PaymentRequestDto paymentRequestDto = new PaymentRequestDto(1L, 1L, 1000L);

            wireMock.stubFor(get(urlPathMatching("/api/v1.0/random.*"))
                    .willReturn(okJson("[20]")));



            mockMvc.perform(MockMvcRequestBuilders.post(CONTROLLER_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(paymentRequestDto)))
                    .andExpect(MockMvcResultMatchers.status().isCreated())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.userId").value(1))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.orderId").value(1))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(Status.SUCCESS.name()))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.paymentAmount").value(1000));

            ConsumerRecord<String, PaymentEventDto> receivedRecord =
                    KafkaTestUtils.getSingleRecord(consumer, topicName, Duration.ofSeconds(5));

            assertNotNull(receivedRecord);
            assertEquals(paymentRequestDto.orderId(), receivedRecord.value().getOrderId());
            assertEquals(Status.SUCCESS, receivedRecord.value().getStatus());

        }

        @Test
        @DisplayName("Should create payment and publish event with FAILED to Kafka")
        void shouldCreatePaymentSuccessfullyStatusFailed() throws Exception {
            PaymentRequestDto paymentRequestDto = new PaymentRequestDto(1L, 1L, 1000L);

            wireMock.stubFor(get(urlPathMatching("/api/v1.0/random.*"))
                    .willReturn(okJson("[21]")));

            mockMvc.perform(MockMvcRequestBuilders.post(CONTROLLER_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(paymentRequestDto)))
                    .andExpect(MockMvcResultMatchers.status().isCreated())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.userId").value(1))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.orderId").value(1))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(Status.FAILED.name()))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.paymentAmount").value(1000));

            ConsumerRecord<String, PaymentEventDto> receivedRecord =
                    KafkaTestUtils.getSingleRecord(consumer, topicName, Duration.ofSeconds(5));

            assertNotNull(receivedRecord);
            assertEquals(paymentRequestDto.orderId(), receivedRecord.value().getOrderId());
            assertEquals(Status.FAILED, receivedRecord.value().getStatus());

        }

        @Test
        @DisplayName("Should return 400 Bad Request when validation fails (null fields)")
        void shouldReturnBadRequestWhenFieldsAreNull() throws Exception {
            PaymentRequestDto invalidDto = new PaymentRequestDto(null, null, null);

            mockMvc.perform(MockMvcRequestBuilders.post(CONTROLLER_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 409 Conflict when payment for order already exists")
        void shouldReturnConflictWhenOrderExists() throws Exception {
            PaymentRequestDto firstRequest = new PaymentRequestDto(1L, 100L, 1000L);

            wireMock.stubFor(get(urlPathMatching("/api/v1.0/random.*"))
                    .willReturn(okJson("[2]")));

            mockMvc.perform(MockMvcRequestBuilders.post(CONTROLLER_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(firstRequest)))
                    .andExpect(MockMvcResultMatchers.status().isCreated());

            mockMvc.perform(MockMvcRequestBuilders.post(CONTROLLER_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(firstRequest)))
                    .andExpect(MockMvcResultMatchers.status().isConflict());
        }

        @Test
        @DisplayName("Should create payment with FAILED status when external API fails")
        void shouldHandleExternalApiFailure() throws Exception {
            PaymentRequestDto requestDto = new PaymentRequestDto(1L, 3L, 1000L);

            wireMock.stubFor(get(urlPathMatching("/api/v1.0/random.*"))
                    .willReturn(serverError()));

            mockMvc.perform(MockMvcRequestBuilders.post(CONTROLLER_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(MockMvcResultMatchers.status().isCreated())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(Status.FAILED.name()));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when body is missing")
        void shouldReturnBadRequestWhenBodyIsMissing() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(CONTROLLER_PATH)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Get Payment Integration Tests")
    class GetPaymentIntegrationTests {

        @Test
        @DisplayName("Should return payment when id exists")
        void shouldReturnPaymentWhenIdExists() throws Exception {
            Payment paymentToSave = Payment.builder()
                    .userId(1L)
                    .orderId(123L)
                    .paymentAmount(5000L)
                    .status(Status.SUCCESS)
                    .build();
            Payment savedPayment = mongoTemplate.save(paymentToSave);
            String savedId = savedPayment.getId();

            mockMvc.perform(MockMvcRequestBuilders.get(CONTROLLER_PATH + "/{id}", savedId))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.userId").value(1))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.orderId").value(123))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(Status.SUCCESS.name()))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.paymentAmount").value(5000));
        }

        @Test
        @DisplayName("Should return 404 Not Found when id does not exist")
        void shouldReturnNotFoundWhenIdDoesNotExist() throws Exception {
            String nonExistentId = "65f1a2b3c4d5e6f7a8b9c0d1";

            mockMvc.perform(MockMvcRequestBuilders.get(CONTROLLER_PATH + "/{id}", nonExistentId))
                    .andExpect(MockMvcResultMatchers.status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 or 404 when id is empty or malformed")
        void shouldHandleInvalidId() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(CONTROLLER_PATH + "/ "))
                    .andExpect(MockMvcResultMatchers.status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Get All Payments Integration Tests")
    class GetAllPaymentsIntegrationTests {

        @Test
        @DisplayName("Should return page of payments with default pagination")
        void shouldReturnPagedPayments() throws Exception {
            List<Payment> payments = List.of(
                    Payment.builder().userId(1L).orderId(101L).paymentAmount(100L).status(Status.SUCCESS).build(),
                    Payment.builder().userId(1L).orderId(102L).paymentAmount(200L).status(Status.FAILED).build()
            );
            mongoTemplate.insertAll(payments);

            mockMvc.perform(MockMvcRequestBuilders.get(CONTROLLER_PATH)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.content.length()").value(2))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.pageable.pageSize").value(15))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(2));
        }

        @Test
        @DisplayName("Should return empty page when no payments exist")
        void shouldReturnEmptyPage() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(CONTROLLER_PATH))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.content").isEmpty())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("Should respect custom pagination parameters")
        void shouldRespectCustomPagination() throws Exception {
            for (long i = 1; i <= 3; i++) {
                mongoTemplate.save(Payment.builder().userId(i).orderId(i).paymentAmount(i * 100).build());
            }

            mockMvc.perform(MockMvcRequestBuilders.get(CONTROLLER_PATH)
                            .param("page", "0")
                            .param("size", "1"))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.content.length()").value(1))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(3));
        }
    }

    @Nested
    @DisplayName("Search Payments Integration Tests")
    class SearchPaymentsIntegrationTests {

        @Test
        @DisplayName("Should return filtered payments when all parameters are provided")
        void shouldReturnFilteredPayments() throws Exception {
            Payment payment1 = Payment.builder()
                    .userId(1L).orderId(101L).paymentAmount(100L).status(Status.SUCCESS).build();
            Payment payment2 = Payment.builder()
                    .userId(2L).orderId(102L).paymentAmount(200L).status(Status.FAILED).build();

            mongoTemplate.insertAll(List.of(payment1, payment2));

            mockMvc.perform(MockMvcRequestBuilders.get(CONTROLLER_PATH + "/search")
                            .param("userId", "1")
                            .param("orderId", "101")
                            .param("status", "SUCCESS"))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.content.length()").value(1))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].orderId").value(101))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].status").value("SUCCESS"));
        }


        @Test
        @DisplayName("Should return empty page when no matches found in DB")
        void shouldReturnEmptyPageWhenNoMatches() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(CONTROLLER_PATH)
                            .param("userId", "999")
                            .param("orderId", "999")
                            .param("status", "FAILED"))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.content").isEmpty())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("Get Sum by Date Range Tests")
    class GetSumByDateRangeTests {

        @Test
        @DisplayName("Should return correct sum when data exists")
        void shouldReturnCorrectSum() throws Exception {
            Long userId = 1L;
            LocalDateTime start = LocalDateTime.now().minusDays(5);
            LocalDateTime end = LocalDateTime.now().plusDays(1);

            Payment payment1 = Payment.builder()
                    .userId(userId).orderId(101L).paymentAmount(100L).status(Status.SUCCESS).build();
            payment1.setTimestamp(LocalDateTime.now().minusDays(2));

            Payment payment2 = Payment.builder()
                    .userId(userId).orderId(101L).paymentAmount(100L).status(Status.SUCCESS).build();
            payment2.setTimestamp(LocalDateTime.now().minusDays(1));

            paymentRepository.saveAll(List.of(payment1, payment2));

            mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/sum")
                            .param("start", start.toString())
                            .param("end", end.toString())
                            .param("userId", userId.toString()))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.content().string("200"));
        }

        @Test
        @DisplayName("Should return 0 when no payments found in range")
        void shouldReturnZeroWhenNoData() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/sum")
                            .param("start", LocalDateTime.now().plusYears(1).toString())
                            .param("end", LocalDateTime.now().plusYears(2).toString())
                            .param("userId", "999"))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.content().string("0"));
        }
    }

    @Nested
    @DisplayName("Change Payment Status Integration Tests")
    class ChangePaymentStatusIntegrationTests {

        @Test
        @DisplayName("Should successfully update payment status")
        void shouldUpdateStatus() throws Exception {
            Payment payment = Payment.builder()
                    .userId(1L)
                    .orderId(555L)
                    .paymentAmount(1000L)
                    .status(Status.PENDING)
                    .build();
            Payment savedPayment = mongoTemplate.save(payment);
            String id = savedPayment.getId();

            Status newStatus = Status.SUCCESS;

            mockMvc.perform(MockMvcRequestBuilders.patch(CONTROLLER_PATH + "/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newStatus)))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(Status.SUCCESS.name()));

            Payment updatedPayment = mongoTemplate.findById(id, Payment.class);
            assertNotNull(updatedPayment);
            assertEquals(Status.SUCCESS, updatedPayment.getStatus());
        }

        @Test
        @DisplayName("Should return 404 Not Found when updating non-existent payment")
        void shouldReturnNotFound() throws Exception {
            String nonExistentId = "65f1a2b3c4d5e6f7a8b9c0d2";

            mockMvc.perform(MockMvcRequestBuilders.patch(CONTROLLER_PATH + "/{id}", nonExistentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Status.FAILED)))
                    .andExpect(MockMvcResultMatchers.status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when status is invalid")
        void shouldReturnBadRequestForInvalidStatus() throws Exception {
            String id = "some-id";

            mockMvc.perform(MockMvcRequestBuilders.patch(CONTROLLER_PATH + "/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("\"INVALID_STATUS_NAME\""))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when body is empty")
        void shouldReturnBadRequestForEmptyBody() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.patch(CONTROLLER_PATH + "/{id}", "id")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Soft Delete Payment Integration Tests")
    class SoftDeletePaymentIntegrationTests {

        @Test
        @DisplayName("Should successfully soft delete payment")
        void shouldSoftDeletePayment() throws Exception {
            Payment payment = Payment.builder()
                    .userId(1L)
                    .orderId(777L)
                    .paymentAmount(3000L)
                    .isDeleted(false)
                    .build();
            Payment savedPayment = mongoTemplate.save(payment);
            String id = savedPayment.getId();

            mockMvc.perform(MockMvcRequestBuilders.delete(CONTROLLER_PATH + "/{id}", id))
                    .andExpect(MockMvcResultMatchers.status().isNoContent());

            Payment deletedPayment = mongoTemplate.findById(id, Payment.class);
            assertNotNull(deletedPayment);
            assertTrue(deletedPayment.isDeleted(), "Поле isDeleted должно быть true после soft delete");
        }

        @Test
        @DisplayName("Should return 404 Not Found when deleting non-existent payment")
        void shouldReturnNotFoundWhenIdDoesNotExist() throws Exception {
            String nonExistentId = "65f1a2b3c4d5e6f7a8b9c0d3";

            mockMvc.perform(MockMvcRequestBuilders.delete(CONTROLLER_PATH + "/{id}", nonExistentId))
                    .andExpect(MockMvcResultMatchers.status().isNotFound());
        }

    }

    private static Consumer<String, PaymentEventDto> createTestConsumer() {
        String uniqueGroupId = "test-group-" + java.util.UUID.randomUUID();

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                kafka.getBootstrapServers(),
                uniqueGroupId,
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