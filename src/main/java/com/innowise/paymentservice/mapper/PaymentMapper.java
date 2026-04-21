package com.innowise.paymentservice.mapper;

import com.innowise.paymentservice.dto.request.PaymentRequestDto;
import com.innowise.paymentservice.dto.response.PaymentResponseDto;
import com.innowise.paymentservice.entity.Payment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    Payment toEnity(PaymentRequestDto paymentRequestDto);

    PaymentResponseDto toResponseDto(Payment payment);
}
