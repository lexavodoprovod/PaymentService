package com.innowise.paymentservice.service.impl;

import com.innowise.paymentservice.client.NumberClient;
import com.innowise.paymentservice.dto.request.PaymentRequestDto;
import com.innowise.paymentservice.dto.response.PaymentResponseDto;
import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.entity.Status;
import com.innowise.paymentservice.exception.payment.PaymentAlreadyExistException;
import com.innowise.paymentservice.exception.payment.PaymentNotFoundException;
import com.innowise.paymentservice.exception.payment.PaymentNullParameterException;
import com.innowise.paymentservice.mapper.PaymentMapper;
import com.innowise.paymentservice.repository.PaymentRepository;
import com.innowise.paymentservice.repository.impl.CustomPaymentRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.innowise.paymentservice.constant.SettingsForNumberClient.*;
@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CustomPaymentRepositoryImpl customPaymentRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private NumberClient numberClient;

    @InjectMocks
    private PaymentServiceImpl paymentService;


    private PaymentRequestDto paymentRequestDto;

    private Payment payment;

    @BeforeEach
    void setUp() {
        paymentRequestDto = new PaymentRequestDto(1L, 1L, 1000L);

        payment = Payment.builder()
                .id("Mongo_ID")
                .orderId(1L)
                .userId(1L)
                .paymentAmount(1000L)
                .build();
    }


    @Nested
    @DisplayName("Create Payment Tests")
    class CreatePaymentTests {

        @Test
        @DisplayName("Should create payment correctly")
        void createPayment() {
            when(paymentRepository.findByOrderId(paymentRequestDto.orderId()))
                    .thenReturn(Optional.empty());

            when(paymentMapper.toEntity(paymentRequestDto))
                    .thenReturn(payment);

            when(numberClient.getRandomNumber(MIN, MAX, COUNT))
                    .thenReturn(List.of(20));

            payment.setStatus(Status.SUCCESS);

            when(paymentRepository.save(payment))
                .thenReturn(payment);

            PaymentResponseDto paymentResponseDto = new PaymentResponseDto(
                    payment.getUserId(),
                    payment.getOrderId(),
                    payment.getStatus(),
                    payment.getPaymentAmount());

            when(paymentMapper.toResponseDto(payment))
                    .thenReturn(paymentResponseDto);


            PaymentResponseDto responseDto = paymentService.createPayment(paymentRequestDto);

            assertNotNull(responseDto);
            assertEquals(paymentResponseDto, responseDto);

            assertTrue(responseDto.status() == Status.SUCCESS);

            verify(paymentRepository).save(argThat(p -> p.getStatus().equals(Status.SUCCESS)));
        }

        @Test
        @DisplayName("Should throw PaymentNullParameterException when DTO is null")
        void shouldThrowExceptionWhenRequestIsNull() {
            assertThrows(PaymentNullParameterException.class, () ->
                    paymentService.createPayment(null)
            );
            verifyNoInteractions(paymentRepository, paymentMapper, numberClient);
        }

        @Test
        @DisplayName("Should throw PaymentAlreadyExistException when orderId already exists")
        void shouldThrowExceptionWhenOrderIdExists() {
            when(paymentRepository.findByOrderId(paymentRequestDto.orderId()))
                    .thenReturn(Optional.of(payment));

            assertThrows(PaymentAlreadyExistException.class, () ->
                    paymentService.createPayment(paymentRequestDto)
            );

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should set SUCCESS status when random number is even")
        void shouldSetSuccessStatusWhenNumberIsEven() {
            when(paymentRepository.findByOrderId(paymentRequestDto.orderId())).thenReturn(Optional.empty());
            when(paymentMapper.toEntity(paymentRequestDto)).thenReturn(payment);
            when(numberClient.getRandomNumber(MIN, MAX, COUNT)).thenReturn(List.of(100));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            paymentService.createPayment(paymentRequestDto);

            verify(paymentRepository).save(argThat(p -> p.getStatus() == Status.SUCCESS));
        }

        @Test
        @DisplayName("Should set FAILED status when random number is odd")
        void shouldSetFailedStatusWhenNumberIsOdd() {
            when(paymentRepository.findByOrderId(paymentRequestDto.orderId())).thenReturn(Optional.empty());
            when(paymentMapper.toEntity(paymentRequestDto)).thenReturn(payment);
            when(numberClient.getRandomNumber(MIN, MAX, COUNT)).thenReturn(List.of(101));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            paymentService.createPayment(paymentRequestDto);

            verify(paymentRepository).save(argThat(p -> p.getStatus() == Status.FAILED));
        }

        @Test
        @DisplayName("Should set FAILED status when numberClient throws exception")
        void shouldSetFailedStatusWhenClientFails() {
            when(paymentRepository.findByOrderId(paymentRequestDto.orderId())).thenReturn(Optional.empty());
            when(paymentMapper.toEntity(paymentRequestDto)).thenReturn(payment);

            when(numberClient.getRandomNumber(MIN, MAX, COUNT)).thenThrow(new RuntimeException("API error"));

            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            paymentService.createPayment(paymentRequestDto);

            verify(paymentRepository).save(argThat(p -> p.getStatus() == Status.FAILED));
        }

        @Test
        @DisplayName("Should set FAILED status when numberClient returns empty list")
        void shouldSetFailedStatusWhenClientReturnsEmptyList() {
            when(paymentRepository.findByOrderId(paymentRequestDto.orderId())).thenReturn(Optional.empty());
            when(paymentMapper.toEntity(paymentRequestDto)).thenReturn(payment);
            when(numberClient.getRandomNumber(MIN, MAX, COUNT)).thenReturn(List.of());

            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            paymentService.createPayment(paymentRequestDto);

            verify(paymentRepository).save(argThat(p -> p.getStatus() == Status.FAILED));
        }
    }

    @Nested
    @DisplayName("Find Payment By Id Tests")
    class FindPaymentByIdTests {

        @Test
        @DisplayName("Should throw PaymentNullParameterException when id is null")
        void shouldThrowExceptionWhenIdIsNull() {
            assertThrows(PaymentNullParameterException.class, () ->
                    paymentService.findPaymentById(null)
            );

            verifyNoInteractions(paymentRepository, paymentMapper);
        }

        @Test
        @DisplayName("Should throw PaymentNotFoundException when payment does not exist")
        void shouldThrowExceptionWhenPaymentNotFound() {
            String id = "non-existent-id";
            when(paymentRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(PaymentNotFoundException.class, () ->
                    paymentService.findPaymentById(id)
            );

            verify(paymentRepository).findById(id);
            verifyNoInteractions(paymentMapper);
        }

        @Test
        @DisplayName("Should return PaymentResponseDto when payment exists")
        void shouldReturnResponseDtoWhenPaymentExists() {
            String id = "Mongo_ID";
            when(paymentRepository.findById(id)).thenReturn(Optional.of(payment));

            PaymentResponseDto expectedResponse = new PaymentResponseDto(
                    payment.getUserId(),
                    payment.getOrderId(),
                    payment.getStatus(),
                    payment.getPaymentAmount()
            );

            when(paymentMapper.toResponseDto(payment)).thenReturn(expectedResponse);

            PaymentResponseDto actualResponse = paymentService.findPaymentById(id);

            assertNotNull(actualResponse);
            assertEquals(expectedResponse.orderId(), actualResponse.orderId());
            assertEquals(expectedResponse.status(), actualResponse.status());

            verify(paymentRepository).findById(id);
            verify(paymentMapper).toResponseDto(payment);
        }
    }




}