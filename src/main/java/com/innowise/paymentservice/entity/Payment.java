package com.innowise.paymentservice.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter@Setter
@Document(collection = "payments")
public class Payment extends BaseEntity{

    @Id
    private String id;

    @Indexed
    @Field(name = "user_id")
    private Long userId;

    @Indexed(unique = true)
    @Field(name = "order_id")
    private Long orderId;

    @Builder.Default
    private Status status = Status.PENDING;

    @Field(name = "payment_amount")
    private Long paymentAmount;

    @Builder.Default
    @Field(name = "deleted")
    @Indexed
    private boolean isDeleted = false;
}
