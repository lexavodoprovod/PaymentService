package com.innowise.paymentservice.exception.payment;

import com.innowise.paymentservice.exception.BusinessException;
import org.springframework.http.HttpStatus;

import static com.innowise.paymentservice.constant.ExceptionMessage.*;
public class PaymentAlreadyExistException extends BusinessException {
    public PaymentAlreadyExistException(Long orderId) {
        super(PAYMENT_ALREADY_EXIST_EXCEPTION_MESSAGE.formatted(orderId), HttpStatus.CONFLICT);
    }
}
