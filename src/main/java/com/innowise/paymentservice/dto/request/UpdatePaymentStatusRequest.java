package com.innowise.paymentservice.dto.request;

import com.innowise.paymentservice.entity.Status;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UpdatePaymentStatusRequest {
    @NotNull
    private Status paymentStatus;
}
