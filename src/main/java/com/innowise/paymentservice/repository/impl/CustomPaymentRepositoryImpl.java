package com.innowise.paymentservice.repository.impl;

import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.repository.CustomPaymentRepository;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
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

        Criteria criteria = Criteria.where("timestamp").gte(start).lte(end)
                .and("deleted").is(false);

        if(userId != null){
            criteria = criteria.and("user_id").is(userId);
        }

        MatchOperation matchStage = Aggregation.match(criteria);

        GroupOperation groupStage = Aggregation.group().sum("payment_amount").as("total");

        Aggregation aggregation = Aggregation.newAggregation(matchStage, groupStage);

        AggregationResults<Document> result = mongoTemplate.aggregate(aggregation, "payments", Document.class);

        return result.getUniqueMappedResult() != null
                ? result.getUniqueMappedResult().getLong("total")
                : 0L;

    }

    @Override
    public List<Payment> getPaymentsByUserIdOrOrderIdOrStatus(Long userId, Long orderId, String status) {

        Criteria mainCriteria = Criteria.where("deleted").is(false);

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
            mainCriteria.orOperator(criteriaList.toArray(new Criteria[0]));
        }

        return mongoTemplate.find(new Query(mainCriteria), Payment.class);
    }

    @Override
    public boolean softDelete(String id) {
        Criteria criteria = Criteria.where("id").is(id);

        Query query = new Query(criteria);

        Update update = new Update();
        update.set("deleted_at", LocalDateTime.now())
                .set("deleted", true);

        UpdateResult result = mongoTemplate.updateFirst(query, update, Payment.class);
        return result.getModifiedCount() > 0;
    }
}
