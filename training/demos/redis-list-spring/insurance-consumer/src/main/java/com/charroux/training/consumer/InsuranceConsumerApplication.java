package com.charroux.training.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InsuranceConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(InsuranceConsumerApplication.class, args);
    }
}
