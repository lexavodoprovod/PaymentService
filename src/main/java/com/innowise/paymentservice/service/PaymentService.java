package com.innowise.paymentservice.service;

import com.innowise.paymentservice.dto.request.PaymentRequestDto;
import com.innowise.paymentservice.dto.response.PaymentResponseDto;
import com.innowise.paymentservice.entity.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

/**
 * Service interface for managing payment operations.
 * Provides functionality for creating, retrieving, updating, and deleting payments,
 * as well as calculating financial statistics.
 */
public interface PaymentService {

    /**
     * Processes and creates a new payment record.
     *
     * @param paymentRequestDto the data transfer object containing payment details
     * @return the created payment as a {@link PaymentResponseDto}
     */
    PaymentResponseDto createPayment(PaymentRequestDto paymentRequestDto);

    /**
     * Retrieves a specific payment by its unique identifier.
     *
     * @param paymentId the unique ID of the payment
     * @return the found payment as a {@link PaymentResponseDto}
     * @throws RuntimeException if no payment is found with the given ID
     */
    PaymentResponseDto findPaymentById(String paymentId);

    /**
     * Retrieves all payments in a paginated format.
     *
     * @param pageable pagination and sorting information
     * @return a {@link Page} of {@link PaymentResponseDto} objects
     */
    Page<PaymentResponseDto> findAllPayments(Pageable pageable);

    /**
     * Searches for payments based on optional criteria: user ID, order ID, or status.
     * Results are returned in a paginated format.
     *
     * @param userId   the ID of the user (optional)
     * @param orderId  the ID of the order (optional)
     * @param status   the current status of the payment (optional)
     * @param pageable pagination and sorting information
     * @return a {@link Page} of filtered {@link PaymentResponseDto} objects
     */
    Page<PaymentResponseDto> findPaymentsByUserIdOrOrderIdOrStatus(Long userId, Long orderId, String status, Pageable pageable);

    /**
     * Calculates the total sum of payments for a specific user within a given date range.
     *
     * @param start  the start date and time of the range
     * @param end    the end date and time of the range
     * @param userId the ID of the user
     * @return the total sum of payments as a {@link Long}
     */
    Long getTotalSumForDateRange(LocalDateTime start, LocalDateTime end, Long userId);

    /**
     * Updates the status of an existing payment.
     *
     * @param paymentId the unique ID of the payment to update
     * @param status    the new {@link Status} to be applied
     * @return the updated payment as a {@link PaymentResponseDto}
     */
    PaymentResponseDto changePaymentStatus(String paymentId, Status status);

    /**
     * Performs a soft delete on a payment record, marking it as inactive or deleted
     * without removing it from the physical database.
     *
     * @param paymentId the unique ID of the payment to delete
     * @return {@code true} if the operation was successful, {@code false} otherwise
     */
    Boolean softDeletePayment(String paymentId);
}