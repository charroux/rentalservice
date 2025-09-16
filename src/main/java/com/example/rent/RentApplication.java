package com.example.rent;

import com.example.rent.data.*;
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
	public CommandLineRunner demo(CarRepository carRepository,
								  PersonRepository personRepository,
								  ContractRepository contractRepository) {
		return (args) -> {
			Car car = new Car();
			car.setPlateNumber("AA11BB");
			carRepository.save(car);

			RentalContract contract1 = new RentalContract();
			RentalContract contract2 = new RentalContract();

			car.getContracts().add(contract1);
			contract1.setCar(car);

			car.getContracts().add(contract2);
			contract2.setCar(car);

			Person tintin = new Person();
			personRepository.save(tintin);
			tintin.getContracts().add(contract1);
			contract1.setPerson(tintin);

			tintin.getContracts().add(contract2);
			contract2.setPerson(tintin);

			contractRepository.save(contract1);
			contractRepository.save(contract2);

		};
	};


}
