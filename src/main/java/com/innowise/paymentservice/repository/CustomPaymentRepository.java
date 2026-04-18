package com.innowise.paymentservice.repository;

import com.innowise.paymentservice.entity.Payment;

import java.time.LocalDateTime;
import java.util.List;

public interface CustomPaymentRepository {

    Long getTotalSumForDateRange(LocalDateTime start, LocalDateTime end, Long userId);
    List<Payment> getPaymentsByUserIdOrOrderIdOrStatus(Long userId, Long orderId, String status);
}
