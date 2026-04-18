package com.innowise.paymentservice.repository.impl;

import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.repository.CustomPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CustomPaymentRepositoryImpl implements CustomPaymentRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public Long getTotalSumForDateRange(LocalDateTime start, LocalDateTime end, Long userId) {

        Criteria criteria = Criteria.where("timestamp").gte(start).lte(end);

        if(userId != null){
            criteria = criteria.and("user_id").is(userId);
        }

        MatchOperation matchStage = Aggregation.match(criteria);

        GroupOperation groupStage = Aggregation.group().sum("payment_amount").as("total");

        Aggregation aggregation = Aggregation.newAggregation(groupStage, matchStage);

        AggregationResults<Document> result = mongoTemplate.aggregate(aggregation, "payments", Document.class);

        return result.getUniqueMappedResult() != null
                ? result.getUniqueMappedResult().getInteger("total").longValue()
                : 0L;

    }

    @Override
    public List<Payment> getPaymentsByUserIdOrOrderIdOrStatus(Long userId, Long orderId, String status) {
        Query query = new Query();

        List<Criteria> criteriaList = new ArrayList<>();

        if(userId != null){
            criteriaList.add(Criteria.where("user_id").is(userId));
        }

        if(orderId != null){
            criteriaList.add(Criteria.where("order_id").is(orderId));
        }

        if(status != null){
            criteriaList.add(Criteria.where("status").is(status));
        }

        if(!criteriaList.isEmpty()){
            query.addCriteria(new Criteria().orOperator(criteriaList.toArray(new Criteria[0])));
        }

        return mongoTemplate.find(query, Payment.class);
    }
}
