package com.example.rent;

import com.example.rent.data.Car;
import com.example.rent.data.CarRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class RentApplication {

	public static void main(String[] args) {
		SpringApplication.run(RentApplication.class, args);
	}

	@Bean
	public CommandLineRunner demo(CarRepository carRepository) {
		return (args) -> {
			Car car = new Car();
			car.setPlateNumber("AA11BB");
			carRepository.save(car);
		};
	};


}
