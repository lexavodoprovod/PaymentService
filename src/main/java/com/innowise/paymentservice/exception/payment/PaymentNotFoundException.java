package com.innowise.paymentservice.exception.payment;

import com.innowise.paymentservice.exception.EntityNotFoundException;


import static com.innowise.paymentservice.constant.ExceptionMessage.*;

public class PaymentNotFoundException extends EntityNotFoundException {
    public PaymentNotFoundException(String id) {
        super(PAYMENT_NOT_FOUND_EXCEPTION_MESSAGE.formatted(id));
    }
}
