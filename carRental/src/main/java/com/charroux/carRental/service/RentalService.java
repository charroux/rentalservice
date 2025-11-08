package com.charroux.carRental.service;

import com.charroux.carRental.entity.CarModelJPA;

import java.util.List;

public interface RentalService {
    public List<CarModelJPA> carsToBeRented();
}
