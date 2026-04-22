package com.innowise.paymentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "random-number-api",
        url = "http://www.randomnumberapi.com/api/v1.0",
        path = "/random"
)
public interface NumberClient {

    @GetMapping()
    List<Integer> getRandomNumber(
            @RequestParam int min,
            @RequestParam int max,
            @RequestParam int count
    );
}
