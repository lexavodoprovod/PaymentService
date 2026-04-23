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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
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
                    payment.getId(),
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
                    payment.getId(),
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

    @Nested
    @DisplayName("Find All Payments Tests")
    class FindAllPaymentsTests {

        @Test
        @DisplayName("Should throw PaymentNullParameterException when pageable is null")
        void shouldThrowExceptionWhenPageableIsNull() {
            assertThrows(PaymentNullParameterException.class, () ->
                    paymentService.findAllPayments(null)
            );

            verifyNoInteractions(paymentRepository, paymentMapper);
        }

        @Test
        @DisplayName("Should return empty Page when no payments found")
        void shouldReturnEmptyPageWhenNoPaymentsExist() {
            Pageable pageable = PageRequest.of(0, 10);
            when(paymentRepository.findAll(pageable)).thenReturn(Page.empty());

            Page<PaymentResponseDto> result = paymentService.findAllPayments(pageable);

            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(paymentRepository).findAll(pageable);
            verifyNoInteractions(paymentMapper);
        }

        @Test
        @DisplayName("Should return Page of DTOs when payments exist")
        void shouldReturnPageOfDtosWhenPaymentsExist() {
            Pageable pageable = PageRequest.of(0, 10);
            List<Payment> payments = List.of(payment);
            Page<Payment> paymentPage = new PageImpl<>(payments, pageable, payments.size());

            PaymentResponseDto responseDto = new PaymentResponseDto(
                    payment.getId(),
                    payment.getUserId(),
                    payment.getOrderId(),
                    payment.getStatus(),
                    payment.getPaymentAmount()
            );

            when(paymentRepository.findAll(pageable)).thenReturn(paymentPage);
            when(paymentMapper.toResponseDto(any(Payment.class))).thenReturn(responseDto);

            Page<PaymentResponseDto> result = paymentService.findAllPayments(pageable);

            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            assertEquals(responseDto, result.getContent().get(0));

            verify(paymentRepository).findAll(pageable);
            verify(paymentMapper, times(1)).toResponseDto(payment);
        }
    }

    @Nested
    @DisplayName("Find Payments By UserId, OrderId or Status Tests")
    class FindPaymentsByUserIdOrOrderIdOrStatusTests {

        @Test
        @DisplayName("Should throw PaymentNullParameterException when pageable is null")
        void shouldThrowExceptionWhenPageableIsNull() {
            assertThrows(PaymentNullParameterException.class, () ->
                    paymentService.findPaymentsByUserIdOrOrderIdOrStatus(1L, 1L, "SUCCESS", null)
            );

            verifyNoInteractions(customPaymentRepository, paymentMapper);
        }

        @Test
        @DisplayName("Should return empty Page when custom repository returns empty")
        void shouldReturnEmptyPageWhenNoMatchesFound() {
            Pageable pageable = PageRequest.of(0, 10);
            Long userId = 1L;
            Long orderId = 2L;
            String status = "PENDING";

            when(customPaymentRepository.getPaymentsByUserIdOrOrderIdOrStatus(userId, orderId, status, pageable))
                    .thenReturn(Page.empty());

            Page<PaymentResponseDto> result = paymentService.findPaymentsByUserIdOrOrderIdOrStatus(userId, orderId, status, pageable);

            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(customPaymentRepository).getPaymentsByUserIdOrOrderIdOrStatus(userId, orderId, status, pageable);
            verifyNoInteractions(paymentMapper);
        }

        @Test
        @DisplayName("Should return Page of DTOs when custom repository finds results")
        void shouldReturnPageOfDtosWhenPaymentsExist() {
            Pageable pageable = PageRequest.of(0, 10);
            Long userId = 1L;
            List<Payment> payments = List.of(payment);
            Page<Payment> paymentPage = new PageImpl<>(payments, pageable, payments.size());

            PaymentResponseDto responseDto = new PaymentResponseDto(
                    payment.getId(),
                    payment.getUserId(),
                    payment.getOrderId(),
                    payment.getStatus(),
                    payment.getPaymentAmount()
            );

            when(customPaymentRepository.getPaymentsByUserIdOrOrderIdOrStatus(userId, null, null, pageable))
                    .thenReturn(paymentPage);
            when(paymentMapper.toResponseDto(payment)).thenReturn(responseDto);

            Page<PaymentResponseDto> result = paymentService.findPaymentsByUserIdOrOrderIdOrStatus(userId, null, null, pageable);

            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            assertEquals(responseDto, result.getContent().getFirst());

            verify(customPaymentRepository).getPaymentsByUserIdOrOrderIdOrStatus(userId, null, null, pageable);
            verify(paymentMapper).toResponseDto(payment);
        }

        @Test
        @DisplayName("Should pass all parameters correctly to custom repository")
        void shouldPassAllParametersToRepository() {
            Pageable pageable = PageRequest.of(0, 5);
            Long userId = 99L;
            Long orderId = 88L;
            String status = "FAILED";

            when(customPaymentRepository.getPaymentsByUserIdOrOrderIdOrStatus(any(), any(), any(), any()))
                    .thenReturn(Page.empty());

            paymentService.findPaymentsByUserIdOrOrderIdOrStatus(userId, orderId, status, pageable);

            verify(customPaymentRepository).getPaymentsByUserIdOrOrderIdOrStatus(
                    eq(userId),
                    eq(orderId),
                    eq(status),
                    eq(pageable)
            );
        }
    }

    @Nested
    @DisplayName("Get Total Sum For Date Range Tests")
    class GetTotalSumForDateRangeTests {

        @Test
        @DisplayName("Should return total sum from custom repository")
        void shouldReturnTotalSumFromRepository() {
            LocalDateTime start = LocalDateTime.now().minusDays(1);
            LocalDateTime end = LocalDateTime.now();
            Long userId = 123L;
            Long expectedSum = 5000L;

            when(customPaymentRepository.getTotalSumForDateRange(start, end, userId))
                    .thenReturn(expectedSum);

            Long actualSum = paymentService.getTotalSumForDateRange(start, end, userId);

            assertEquals(expectedSum, actualSum);
            verify(customPaymentRepository).getTotalSumForDateRange(start, end, userId);
        }

        @Test
        @DisplayName("Should return null or zero when repository returns it")
        void shouldReturnNullWhenRepositoryReturnsNull() {
            when(customPaymentRepository.getTotalSumForDateRange(any(), any(), any()))
                    .thenReturn(null);

            Long result = paymentService.getTotalSumForDateRange(null, null, null);

            assertNull(result);
            verify(customPaymentRepository).getTotalSumForDateRange(null, null, null);
        }
    }

    @Nested
    @DisplayName("Soft Delete Payment Tests")
    class SoftDeletePaymentTests {

        @Test
        @DisplayName("Should throw PaymentNullParameterException when id is null")
        void shouldThrowExceptionWhenIdIsNull() {
            assertThrows(PaymentNullParameterException.class, () ->
                    paymentService.softDeletePayment(null)
            );

            verifyNoInteractions(paymentRepository, customPaymentRepository);
        }

        @Test
        @DisplayName("Should throw PaymentNotFoundException when payment does not exist")
        void shouldThrowExceptionWhenPaymentNotFound() {
            String id = "non-existent-id";
            when(paymentRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(PaymentNotFoundException.class, () ->
                    paymentService.softDeletePayment(id)
            );

            verify(paymentRepository).findById(id);
            verifyNoInteractions(customPaymentRepository);
        }

        @Test
        @DisplayName("Should return true when soft delete is successful")
        void shouldReturnTrueWhenSoftDeleteIsSuccessful() {
            String id = "Mongo_ID";
            when(paymentRepository.findById(id)).thenReturn(Optional.of(payment));
            when(customPaymentRepository.softDelete(id)).thenReturn(true);

            Boolean result = paymentService.softDeletePayment(id);

            assertTrue(result);
            verify(paymentRepository).findById(id);
            verify(customPaymentRepository).softDelete(id);
        }

        @Test
        @DisplayName("Should return false when soft delete fails in repository")
        void shouldReturnFalseWhenSoftDeleteFails() {
            String id = "Mongo_ID";
            when(paymentRepository.findById(id)).thenReturn(Optional.of(payment));
            when(customPaymentRepository.softDelete(id)).thenReturn(false);

            Boolean result = paymentService.softDeletePayment(id);

            assertFalse(result);
            verify(paymentRepository).findById(id);
            verify(customPaymentRepository).softDelete(id);
        }
    }
    @Nested
    @DisplayName("Change Payment Status Tests")
    class ChangePaymentStatusTests {

        @Test
        @DisplayName("Should throw PaymentNullParameterException when paymentId is null")
        void shouldThrowExceptionWhenIdIsNull() {
            assertThrows(PaymentNullParameterException.class, () ->
                    paymentService.changePaymentStatus(null, Status.SUCCESS)
            );
            verifyNoInteractions(paymentRepository, paymentMapper);
        }

        @Test
        @DisplayName("Should throw PaymentNullParameterException when status is null")
        void shouldThrowExceptionWhenStatusIsNull() {
            assertThrows(PaymentNullParameterException.class, () ->
                    paymentService.changePaymentStatus("some-id", null)
            );
            verifyNoInteractions(paymentRepository, paymentMapper);
        }

        @Test
        @DisplayName("Should throw PaymentNotFoundException when payment does not exist")
        void shouldThrowExceptionWhenPaymentNotFound() {
            String id = "non-existent-id";
            when(paymentRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(PaymentNotFoundException.class, () ->
                    paymentService.changePaymentStatus(id, Status.SUCCESS)
            );

            verify(paymentRepository).findById(id);
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should successfully change status and return DTO")
        void shouldChangeStatusSuccessfully() {
            String id = "Mongo_ID";
            Status newStatus = Status.FAILED;

            when(paymentRepository.findById(id)).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PaymentResponseDto expectedResponse = new PaymentResponseDto(
                    payment.getId(),
                    payment.getUserId(),
                    payment.getOrderId(),
                    newStatus,
                    payment.getPaymentAmount()
            );

            when(paymentMapper.toResponseDto(any(Payment.class))).thenReturn(expectedResponse);

            PaymentResponseDto result = paymentService.changePaymentStatus(id, newStatus);

            assertNotNull(result);
            assertEquals(newStatus, result.status());

            verify(paymentRepository).save(argThat(p -> p.getStatus() == newStatus));
            verify(paymentMapper).toResponseDto(argThat(p -> p.getStatus() == newStatus));
        }
    }





}