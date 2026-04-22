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
import com.innowise.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import static com.innowise.paymentservice.constant.SettingsForNumberClient.*;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final CustomPaymentRepositoryImpl customPaymentRepository;
    private final PaymentMapper paymentMapper;
    private final NumberClient numberClient;


    @Override
    public PaymentResponseDto createPayment(PaymentRequestDto paymentRequestDto) {
        if(paymentRequestDto == null){
            throw new PaymentNullParameterException();
        }

        Long orderId = paymentRequestDto.orderId();

        paymentRepository.findByOrderId(orderId)
                .ifPresent(p -> {
                    throw new PaymentAlreadyExistException(orderId);
                });

        Payment payment = paymentMapper.toEntity(paymentRequestDto);

        try{
            List<Integer> result = numberClient.getRandomNumber(MIN, MAX, COUNT);

            int currentNumber = result.getFirst();

            if(currentNumber % 2 == 0){
                payment.setStatus(Status.SUCCESS);
            }else{
                payment.setStatus(Status.FAILED);
            }
        }catch (Exception e){
            payment.setStatus(Status.FAILED);
        }

        Payment savedPayment = paymentRepository.save(payment);

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
