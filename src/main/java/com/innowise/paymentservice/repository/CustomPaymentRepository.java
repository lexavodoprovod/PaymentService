package com.innowise.paymentservice.repository;

import com.innowise.paymentservice.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface CustomPaymentRepository {

    Long getTotalSumForDateRange(LocalDateTime start, LocalDateTime end, Long userId);
    Page<Payment> getPaymentsByUserIdOrOrderIdOrStatus(Long userId, Long orderId, String status, Pageable pageable);
    boolean softDelete(String id);
}
