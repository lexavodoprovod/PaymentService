package com.innowise.paymentservice.exception.payment;

import com.innowise.paymentservice.exception.BusinessException;
import org.springframework.http.HttpStatus;

import static com.innowise.paymentservice.constant.ExceptionMessage.*;


public class PaymentNullParameterException extends BusinessException {
    public PaymentNullParameterException() {
        super(PAYMENT_NULL_PARAMETER_EXCEPTION_MESSAGE, HttpStatus.BAD_REQUEST);
    }
}
