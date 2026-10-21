package com.charroux.rentalQueryService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RentalQueryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RentalQueryServiceApplication.class, args);
    }
}
