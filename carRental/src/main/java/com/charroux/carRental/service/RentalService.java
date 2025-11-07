package com.charroux.carRental.service;

import com.charroux.carRental.entity.Car;
import com.charroux.carRental.entity.RentalAgreement;
import com.charroux.carRental.web.CarNotFoundException;
import com.charroux.carRental.web.CustomerNotFoundException;

import java.util.Collection;
import java.util.List;

public interface RentalService {
    public RentalAgreement rent(long customerId, int numberOfCars) throws CarNotFoundException;
    public List<Car> carsToBeRented();
    public Car getCar(String plateNumber) throws CarNotFoundException;
    public List<RentalAgreement> getAgreements();
    public RentalAgreement getAgreement(long customerId) throws CustomerNotFoundException;
    public Collection<Car> getCars(RentalAgreement rentalAgreement);

    // Broadcast available cars' plate numbers to websocket subscribers
    public void broadcastAvailableCars();
}
