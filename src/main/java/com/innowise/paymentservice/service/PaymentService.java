package com.innowise.paymentservice.service;

import com.innowise.paymentservice.dto.request.PaymentRequestDto;
import com.innowise.paymentservice.dto.response.PaymentResponseDto;
import com.innowise.paymentservice.entity.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentService {

    PaymentResponseDto createPayment(PaymentRequestDto paymentRequestDto);

    PaymentResponseDto findPaymentById(String paymentId);

    Page<PaymentResponseDto> findAllPayments(Pageable pageable);

    Page<PaymentResponseDto> findPaymentsByUserIdOrOrderIdOrStatus(Long userId, Long orderId, String status, Pageable pageable);

    PaymentResponseDto changePaymentStatus(String paymentId, Status status);

    Boolean softDeletePayment(String paymentId);
}
