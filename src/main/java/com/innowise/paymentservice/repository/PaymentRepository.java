package com.innowise.paymentservice.repository;

import com.innowise.paymentservice.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {

    @Query("{ '_id' : ?0, 'deleted' : false }")
    Optional<Payment> findById(String id);

    @Query("{ 'deleted' : false}")
    Page<Payment> findAll(Pageable pageable);

    @Query("{ 'order_id' : ?0, 'deleted' : false }")
    Optional<Payment> findByOrderId(Long orderId);
}
