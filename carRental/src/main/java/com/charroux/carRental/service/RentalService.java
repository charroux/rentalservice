package com.charroux.carRental.service;

import com.charroux.carRental.entity.Car;
import com.charroux.carRental.entity.CarModelJPA;

import java.util.List;

public interface RentalService {
    public List<CarModelJPA> carsToBeRented();
    public List<Car> getAvailableCars();
    public Car getCarByPlateNumber(String plateNumber);
    public Car participateInAuction(String brand, String model, String companyId);
}
