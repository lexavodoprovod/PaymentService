package com.innowise.paymentservice.dto.kafkadto;

import com.innowise.paymentservice.entity.EventType;
import com.innowise.paymentservice.entity.Status;
import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter@Setter
public class PaymentEventDto {
    private EventType eventType;
    private String paymentId;
    private Long orderId;
    private Status status;
}
