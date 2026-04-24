package com.innowise.paymentservice.repository.impl;

import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.repository.CustomPaymentRepository;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.innowise.paymentservice.constant.DbParameters.*;

@Repository
@RequiredArgsConstructor
public class CustomPaymentRepositoryImpl implements CustomPaymentRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public Long getTotalSumForDateRange(LocalDateTime start, LocalDateTime end, Long userId) {

        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.where(DELETED_FIELD).is(false));

        if(start != null){
            criteriaList.add(Criteria.where(CREATED_AT_FIELD).gte(start));
        }

        if(end != null){
            criteriaList.add(Criteria.where(CREATED_AT_FIELD).lte(end));
        }

        if(userId != null){
            criteriaList.add(Criteria.where(USER_ID_FIELD).is(userId));
        }

        Criteria mainCriteria = new Criteria();
        if (!criteriaList.isEmpty()) {
            mainCriteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }

        MatchOperation matchStage = Aggregation.match(mainCriteria);

        GroupOperation groupStage = Aggregation.group().sum(PAYMENT_AMOUNT_FIELD).as(TOTAL_FIELD);

        Aggregation aggregation = Aggregation.newAggregation(matchStage, groupStage);

        AggregationResults<Document> result = mongoTemplate.aggregate(aggregation, PAYMENTS_COLLECTION_NAME, Document.class);

        return result.getUniqueMappedResult() != null
                ? result.getUniqueMappedResult().getLong(TOTAL_FIELD)
                : 0L;

    }

    @Override
    public Page<Payment> getPaymentsByUserIdOrOrderIdOrStatus(Long userId, Long orderId, String status, Pageable pageable) {

        Criteria mainCriteria = Criteria.where(DELETED_FIELD).is(false);

        List<Criteria> criteriaList = new ArrayList<>();

        if(userId != null){
            criteriaList.add(Criteria.where(USER_ID_FIELD).is(userId));
        }

        if(orderId != null){
            criteriaList.add(Criteria.where(ORDER_ID_FIELD).is(orderId));
        }

        if(status != null){
            criteriaList.add(Criteria.where(STATUS_FIELD).is(status));
        }

        if(!criteriaList.isEmpty()){
            mainCriteria.orOperator(criteriaList.toArray(new Criteria[0]));
        }

        Query query = new Query(mainCriteria).with(pageable);

        List<Payment> payments = mongoTemplate.find(query, Payment.class);

        return PageableExecutionUtils.getPage(
                payments,
                pageable,
                () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Payment.class)
        );
    }

    @Override
    public boolean softDelete(String id) {
        Criteria criteria = Criteria.where(ID_FIELD).is(id);

        Query query = new Query(criteria);

        Update update = new Update();
        update.set(DELETED_FIELD, true);

        UpdateResult result = mongoTemplate.updateFirst(query, update, Payment.class);
        return result.getModifiedCount() > 0;
    }
}
