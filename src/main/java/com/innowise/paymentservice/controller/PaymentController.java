package com.innowise.paymentservice.controller;

import com.innowise.paymentservice.dto.request.PaymentRequestDto;
import com.innowise.paymentservice.dto.response.PaymentResponseDto;
import com.innowise.paymentservice.entity.Status;
import com.innowise.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping(value = "/api/v1/payments", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class PaymentController {

    private static final int PAGINATION_SIZE = 15;
    private static final String SORT_BY = "id";

    private final PaymentService paymentService;

    /**
     * Creates a new payment record.
     *
     * @param paymentRequestDto the payment details to be saved, must be valid
     * @return {@link ResponseEntity} containing the created {@link PaymentResponseDto}
     * and HTTP status 201 (Created)
     */
    @PostMapping
    public ResponseEntity<PaymentResponseDto> addPayment(@Valid @RequestBody PaymentRequestDto paymentRequestDto) {
        PaymentResponseDto paymentResponseDto = paymentService.createPayment(paymentRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentResponseDto);
    }

    /**
     * Retrieves a single payment by its unique ID.
     *
     * @param id the unique identifier of the payment
     * @return {@link ResponseEntity} containing the {@link PaymentResponseDto}
     * and HTTP status 200 (OK)
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponseDto> getPayment(@PathVariable String id) {
        PaymentResponseDto paymentResponseDto = paymentService.findPaymentById(id);
        return ResponseEntity.ok(paymentResponseDto);
    }

    /**
     * Retrieves a paginated list of all payments.
     *
     * @param pageable pagination parameters (page, size, sort)
     * @return {@link ResponseEntity} containing a {@link Page} of {@link PaymentResponseDto}
     */
    @GetMapping
    public ResponseEntity<Page<PaymentResponseDto>> getPayments(
            @PageableDefault(size = PAGINATION_SIZE, sort = SORT_BY) Pageable pageable
    ) {
        Page<PaymentResponseDto> responseDtoPage = paymentService.findAllPayments(pageable);
        return ResponseEntity.ok(responseDtoPage);
    }

    /**
     * Searches for payments based on specific criteria including user ID, order ID, and status.
     *
     * @param userId   the ID of the user associated with the payments
     * @param orderId  the ID of the order associated with the payments
     * @param status   the payment status to filter by
     * @param pageable pagination parameters
     * @return {@link ResponseEntity} containing a {@link Page} of filtered {@link PaymentResponseDto}
     */
    @GetMapping("/search")
    public ResponseEntity<Page<PaymentResponseDto>> getPaymentsByUserIdOrOrderIdOrStatus(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Status status,
            @PageableDefault(size = PAGINATION_SIZE, sort = SORT_BY) Pageable pageable
    ){
        Page<PaymentResponseDto> responseDtoPage = paymentService.findPaymentsByUserIdOrOrderIdOrStatus(
                userId,
                orderId,
                status.name(),
                pageable
        );
        return ResponseEntity.ok(responseDtoPage);
    }

    /**
     * Calculates the total sum of all payments for a specific user within a date range.
     *
     * @param start  the start of the date range (inclusive)
     * @param end    the end of the date range (inclusive)
     * @param userId the ID of the user
     * @return {@link ResponseEntity} containing the total sum as a {@link Long}
     */
    @GetMapping("/sum")
    public ResponseEntity<Long> getSumByDateRange(
            @RequestParam(required = false) LocalDateTime start,
            @RequestParam(required = false) LocalDateTime end,
            @RequestParam(required = false) Long userId
    ){
        Long sum = paymentService.getTotalSumForDateRange(start, end, userId);
        return ResponseEntity.ok(sum);
    }

    /**
     * Updates the status of an existing payment.
     *
     * @param id     the ID of the payment to update
     * @param status the new status to be assigned
     * @return {@link ResponseEntity} containing the updated {@link PaymentResponseDto}
     */
    @PatchMapping("/{id}")
    public ResponseEntity<PaymentResponseDto> changePaymentStatus(
            @PathVariable String id,
            @RequestBody Status status
    ){
        PaymentResponseDto paymentResponseDto = paymentService.changePaymentStatus(id, status);
        return ResponseEntity.ok(paymentResponseDto);
    }

    /**
     * Performs a soft delete on a payment record.
     *
     * @param id the ID of the payment to be deleted
     * @return {@link ResponseEntity} with HTTP status 204 (No Content) if successful,
     * or 404 (Not Found) if the payment does not exist
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable String id){
        boolean success = paymentService.softDeletePayment(id);
        return success ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}