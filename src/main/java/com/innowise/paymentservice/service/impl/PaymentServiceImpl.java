package com.innowise.paymentservice.service.impl;

import com.innowise.paymentservice.dto.request.PaymentRequestDto;
import com.innowise.paymentservice.dto.response.PaymentResponseDto;
import com.innowise.paymentservice.entity.Status;
import com.innowise.paymentservice.mapper.PaymentMapper;
import com.innowise.paymentservice.repository.PaymentRepository;
import com.innowise.paymentservice.repository.impl.CustomPaymentRepositoryImpl;
import com.innowise.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final CustomPaymentRepositoryImpl customPaymentRepository;
    private final PaymentMapper paymentMapper;

    @Override
    public PaymentResponseDto createPayment(PaymentRequestDto paymentRequestDto) {
        return null;
    }

    @Override
    public PaymentResponseDto findPaymentById(String paymentId) {
        return null;
    }

    @Override
    public Page<PaymentResponseDto> findAllPayments(Pageable pageable) {
        return null;
    }

    @Override
    public Page<PaymentResponseDto> findPaymentsByUserIdOrOrderIdOrStatus(Long userId, Long orderId, String status, Pageable pageable) {
        return null;
    }

    @Override
    public PaymentResponseDto changePaymentStatus(String paymentId, Status status) {
        return null;
    }

    @Override
    public Boolean softDeletePayment(String paymentId) {
        return null;
    }
}
