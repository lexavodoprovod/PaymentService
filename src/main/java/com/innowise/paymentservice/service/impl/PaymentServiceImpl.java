package com.innowise.paymentservice.service.impl;

import com.innowise.paymentservice.client.NumberClient;
import com.innowise.paymentservice.dto.kafkadto.PaymentEventDto;
import com.innowise.paymentservice.dto.request.PaymentRequestDto;
import com.innowise.paymentservice.dto.response.PaymentResponseDto;
import com.innowise.paymentservice.entity.EventType;
import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.entity.Status;
import com.innowise.paymentservice.exception.payment.PaymentAlreadyExistException;
import com.innowise.paymentservice.exception.payment.PaymentNotFoundException;
import com.innowise.paymentservice.exception.payment.PaymentNullParameterException;
import com.innowise.paymentservice.mapper.PaymentMapper;
import com.innowise.paymentservice.repository.CustomPaymentRepository;
import com.innowise.paymentservice.repository.PaymentRepository;
import com.innowise.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import static com.innowise.paymentservice.constant.SettingsForNumberClient.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final CustomPaymentRepository customPaymentRepository;
    private final PaymentMapper paymentMapper;
    private final NumberClient numberClient;
    private final KafkaTemplate<String, PaymentEventDto> kafkaTemplate;

    @Value("${topic-name.status}")
    private String statusTopicName;



    @Override
    public PaymentResponseDto createPayment(PaymentRequestDto paymentRequestDto) {
        if(paymentRequestDto == null){
            throw new PaymentNullParameterException();
        }

        Long orderId = paymentRequestDto.orderId();
        log.info("Get Order Id: {}", orderId);

        log.info("Start searching order with this id: {}", orderId);
        paymentRepository.findByOrderId(orderId)
                .ifPresent(p -> {
                    throw new PaymentAlreadyExistException(orderId);
                });
        log.info("Order with that id not found: {}", orderId);

        Payment payment = paymentMapper.toEntity(paymentRequestDto);
        log.info("PaymentRequestDto mapped to Payment: {}", payment);

        try{
            log.info("Try to get number from numberClient");
            List<Integer> result = numberClient.getRandomNumber(MIN, MAX, COUNT);
            log.info("Number from numberClient received");
            int currentNumber = result.getFirst();
            log.info("Number is: {}", currentNumber);

            if(currentNumber % 2 == 0){
                payment.setStatus(Status.SUCCESS);
                log.info("Set payment status SUCCESS");
            }else{
                payment.setStatus(Status.FAILED);
                log.info("Set payment status FAILED");
            }
        }catch (Exception e){
            payment.setStatus(Status.FAILED);
            log.info("Set payment status FAILED after exception");
        }

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment saved with id: {}", savedPayment.getId());

        PaymentEventDto eventDto = PaymentEventDto.builder()
                .eventType(EventType.CREATE_PAYMENT)
                .paymentId(savedPayment.getId())
                .status(savedPayment.getStatus())
                .orderId(orderId)
                .build();
        log.info("Payment event created: {}", eventDto);

        log.info("Try to send kafka event");
        kafkaTemplate.send(statusTopicName, eventDto);
        log.info("Send kafka event successfully");

        log.info("Mapping savedPayment to PaymentResponseDto");
        return paymentMapper.toResponseDto(savedPayment);
    }

    @Override
    public PaymentResponseDto findPaymentById(String paymentId) {

        if(paymentId == null){
            throw new PaymentNullParameterException();
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        return paymentMapper.toResponseDto(payment);
    }

    @Override
    public Page<PaymentResponseDto> findAllPayments(Pageable pageable) {
        if(pageable == null){
            throw new PaymentNullParameterException();
        }

        Page<Payment> paymentsPage = paymentRepository.findAll(pageable);

        return paymentsPage.map(paymentMapper::toResponseDto);
    }

    @Override
    public Page<PaymentResponseDto> findPaymentsByUserIdOrOrderIdOrStatus(
            Long userId,
            Long orderId,
            String status,
            Pageable pageable
    ) {

        if(pageable == null){
            throw new PaymentNullParameterException();
        }

        Page<Payment> paymentsPage = customPaymentRepository.getPaymentsByUserIdOrOrderIdOrStatus(
                userId,
                orderId,
                status,
                pageable
        );

        return paymentsPage.map(paymentMapper::toResponseDto);
    }

    @Override
    public Long getTotalSumForDateRange(LocalDateTime start, LocalDateTime end, Long userId) {
        return customPaymentRepository.getTotalSumForDateRange(start, end, userId);
    }

    @Override
    public PaymentResponseDto changePaymentStatus(String paymentId, Status status) {
        if(paymentId == null || status == null){
            throw new PaymentNullParameterException();
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        payment.setStatus(status);

        Payment updatedPayment = paymentRepository.save(payment);

        return paymentMapper.toResponseDto(updatedPayment);
    }

    @Override
    public Boolean softDeletePayment(String paymentId) {
        if(paymentId == null){
            throw new PaymentNullParameterException();
        }

        paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        return customPaymentRepository.softDelete(paymentId);
    }
}
