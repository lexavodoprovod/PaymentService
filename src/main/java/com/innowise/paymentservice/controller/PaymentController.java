package com.innowise.paymentservice.controller;


import com.innowise.paymentservice.dto.request.PaymentRequestDto;
import com.innowise.paymentservice.dto.response.PaymentResponseDto;
import com.innowise.paymentservice.entity.Status;
import com.innowise.paymentservice.service.impl.PaymentServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.jaxb.SpringDataJaxb;
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

    private final PaymentServiceImpl paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponseDto> addPayment(@Valid @RequestBody PaymentRequestDto paymentRequestDto) {
        PaymentResponseDto paymentResponseDto = paymentService.createPayment(paymentRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentResponseDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponseDto> getPayment(@PathVariable String id) {
        PaymentResponseDto paymentResponseDto = paymentService.findPaymentById(id);
        return ResponseEntity.ok(paymentResponseDto);
    }

    @GetMapping
    public ResponseEntity<Page<PaymentResponseDto>> getPayments(
            @PageableDefault(size = PAGINATION_SIZE, sort = SORT_BY) Pageable pageable
    ) {
        Page<PaymentResponseDto> responseDtoPage = paymentService.findAllPayments(pageable);

        return ResponseEntity.ok(responseDtoPage);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<PaymentResponseDto>> getPaymentsByUserIdOrOrderIdOrStatus(
            @RequestParam Long userId,
            @RequestParam Long orderId,
            @RequestParam Status status,
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

    @GetMapping("/sum")
    public ResponseEntity<Long> getSumByDateRange(
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end,
            @RequestParam Long userId
    ){
        Long sum = paymentService.getTotalSumForDateRange(start, end, userId);

        return ResponseEntity.ok(sum);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PaymentResponseDto> changePaymentStatus(
            @PathVariable String id,
            @RequestBody Status status
    ){
        PaymentResponseDto paymentResponseDto = paymentService.changePaymentStatus(id, status);
        return ResponseEntity.ok(paymentResponseDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable String id){
        boolean success = paymentService.softDeletePayment(id);
        return success ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
