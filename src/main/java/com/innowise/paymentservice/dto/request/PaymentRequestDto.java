package com.innowise.paymentservice.dto.request;

import jakarta.validation.constraints.NotNull;

public record PaymentRequestDto(
        @NotNull
        Long userId,

        @NotNull
        Long orderId,

        @NotNull
        Long paymentAmount
){}
