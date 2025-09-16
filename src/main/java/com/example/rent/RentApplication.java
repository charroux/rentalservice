package com.example.rent;

import com.example.rent.data.Car;
import com.example.rent.data.CarRepository;
import com.example.rent.data.Person;
import com.example.rent.data.PersonRepository;
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
	public CommandLineRunner demo(CarRepository carRepository, PersonRepository personRepository) {
		return (args) -> {
			Car car = new Car();
			car.setPlateNumber("AA11BB");
			carRepository.save(car);

			Person tintin = new Person();
			Person haddock = new Person();

			car.getPersons().add(tintin);
			car.getPersons().add(haddock);

			carRepository.save(car);
			//personRepository.save(tintin);
			//personRepository.save(haddock);

		};
	};


}
