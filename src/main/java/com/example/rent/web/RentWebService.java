package com.example.rent.web;

import com.example.rent.Dates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
public class RentWebService {

    Logger logger = LoggerFactory.getLogger(RentWebService.class);

    @GetMapping("/hello")
    public String sayHello() {
        return "Hello !";
    }

    @PutMapping("/cars/{plateNumber}")
    public void rent(
            @PathVariable("plateNumber")String plaque,
            @RequestParam("rent")boolean toRent,
            @RequestBody Dates dates){

        logger.info("Plaque : " + plaque);
        logger.info("rent : " + toRent);
        logger.info("dates : " + dates);

    }

}
