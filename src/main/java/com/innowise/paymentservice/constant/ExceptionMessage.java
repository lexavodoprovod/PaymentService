package com.innowise.paymentservice.constant;

public final class ExceptionMessage {

    private ExceptionMessage() {}

    public static final String PAYMENT_NULL_PARAMETER_EXCEPTION_MESSAGE =
            "Try to use null parameter in PaymentService";

    public static final String PAYMENT_NOT_FOUND_EXCEPTION_MESSAGE =
            "Could not find payment with id: %s";

    public static final String PAYMENT_SOFT_DELETE_EXCEPTION_MESSAGE =
            "Could not delete payment with id: %s";

    public static final String PAYMENT_ALREADY_EXIST_EXCEPTION_MESSAGE =
            "Payment with this order id [%s] already exists";
}
