package com.innowise.paymentservice.dto.kafkadto;

import com.innowise.paymentservice.entity.Status;

public record PaymentEventDto (
        Long orderId,
        Status status
) {}
