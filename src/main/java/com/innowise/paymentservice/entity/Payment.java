package com.innowise.paymentservice.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import static com.innowise.paymentservice.constant.DbParameters.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter@Setter
@Document(collection = PAYMENTS_COLLECTION_NAME)
public class Payment extends BaseEntity{

    @Id
    private String id;

    @Indexed
    @Field(name = USER_ID_FIELD)
    private Long userId;

    @Indexed(unique = true)
    @Field(name = ORDER_ID_FIELD)
    private Long orderId;

    @Indexed
    @Builder.Default
    private Status status = Status.PENDING;

    @Indexed
    @Field(name = PAYMENT_AMOUNT_FIELD)
    private Long paymentAmount;

    @Builder.Default
    @Field(name = DELETED_FIELD)
    @Indexed
    private boolean isDeleted = false;
}
