package com.innowise.paymentservice.dto.response;

import com.innowise.paymentservice.entity.Status;

public record PaymentResponseDto (
        String id,

        Long userId,

        Long orderId,

        Status status,

        Long paymentAmount
){}
