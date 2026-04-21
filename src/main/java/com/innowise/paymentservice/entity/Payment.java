package com.innowise.paymentservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
