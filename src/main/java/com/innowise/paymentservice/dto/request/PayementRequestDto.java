package com.innowise.paymentservice.dto.request;

import jakarta.validation.constraints.NotNull;

public class PayementRequestDto {

    @NotNull
    private Long userId;

    @NotNull
    private Long orderId;

    @NotNull
    private Long paymentAmount;

}
