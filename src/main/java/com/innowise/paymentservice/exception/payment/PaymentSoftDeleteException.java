package com.innowise.paymentservice.exception.payment;

import com.innowise.paymentservice.exception.BusinessException;
import org.springframework.http.HttpStatus;

import static com.innowise.paymentservice.constant.ExceptionMessage.*;

public class PaymentSoftDeleteException extends BusinessException {
    public PaymentSoftDeleteException(String id) {
        super(PAYMENT_SOFT_DELETE_EXCEPTION_MESSAGE.formatted(id), HttpStatus.CONFLICT);
    }
}
