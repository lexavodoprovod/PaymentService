package com.innowise.paymentservice.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

import static com.innowise.paymentservice.constant.DbParameters.*;

@Getter
@Setter
public class BaseEntity {

    @Indexed
    @CreatedDate
    @Field(name = TIMESTAMP_FIELD)
    private LocalDateTime timestamp;

    @LastModifiedDate
    @Field(name = UPDATED_AT_FIELD)
    private LocalDateTime updatedAt;
}
