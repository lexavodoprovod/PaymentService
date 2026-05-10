package com.innowise.paymentservice.client.fallback;

import com.innowise.paymentservice.client.NumberClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NumberClientFallBack implements NumberClient {
    @Override
    public List<Integer> getRandomNumber(int min, int max, int count) {
        return List.of(0);
    }
}
