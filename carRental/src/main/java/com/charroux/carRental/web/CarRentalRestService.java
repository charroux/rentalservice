package com.charroux.carRental.web;

import com.charroux.carRental.entity.Car;
import com.charroux.carRental.entity.RentalAgreement;
import com.charroux.carRental.service.RentalService;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
//import io.grpc.carservice.CarRentalServiceGrpc;
//import io.grpc.carservice.Invoice;
//import io.grpc.carservice.Car;

import java.util.Collection;
import java.util.List;

@RestController
public class CarRentalRestService {

    RentalService rentalService;
    Logger logger = org.slf4j.LoggerFactory.getLogger(CarRentalRestService.class);

    @Autowired
    public CarRentalRestService(RentalService rentalService) {
        super();
        this.rentalService = rentalService;
    }

    @GetMapping("/cars")
    public List<Car> getListOfCars(){
        logger.info("Fetching list of cars to be rented");      
        return rentalService.carsToBeRented();
    }

    @GetMapping("/cars/{plateNumber}")
    public Car getCarByPlateNumber(@PathVariable("plateNumber") String plateNumber) throws CarNotFoundException {
        return rentalService.getCar(plateNumber);
    }

    @PostMapping("/carsForAgreement")
    public Collection<Car> carsForAgreement(@RequestBody RentalAgreement rentalAgreement) {
        return rentalService.getCars(rentalAgreement);
    }

   /** @PostMapping("/carsForAgreements")
    public Map<RentalAgreement, Collection<Car>> carsForAgreements(@RequestBody List<RentalAgreement> agreements) {
        return agreements.stream()
                .collect(
                        Collectors.toMap(
                                Function.identity(),
                                rentalAgreement ->
                                        rentalService.getCars(rentalAgreement)));
    }*/

   /**  @PostMapping("/cars")
    public ResponseEntity<RentCarsResponse>  rentCars(@RequestBody RentCarCommand rentCarsRequest) throws Exception{
        RentalAgreement rentalAgreement = rentalService.rent(
                rentCarsRequest.getCustomerId(),
                rentCarsRequest.getNumberOfCars());
        return new ResponseEntity<>(
                new RentCarsResponse(
                        rentalAgreement.getCustomerId(), rentalAgreement.getId(), rentalAgreement.getState().name()),
                HttpStatus.OK);
    }*/

    @GetMapping("/agreements")
    public List<RentalAgreement> getAgreements(){
        return rentalService.getAgreements();
    }

    @GetMapping("/agreement")
    public RentalAgreement getAgreement(@RequestParam(value = "customerId", required = true) int customerId) throws CustomerNotFoundException {
        return rentalService.getAgreement(customerId);
    }

    @PostMapping("/cars/{plateNumber}")
    public ResponseEntity<Void> submitApplication(
            @PathVariable("plateNumber") String plateNumber,
            @RequestParam(value = "firstName", required = true) String firstName,
            @RequestParam(value = "lastName", required = true) String lastName,
            @RequestParam(value = "email", required = true) String email,
            @RequestParam(value = "beginDate", required = true) String beginDate,
            @RequestParam(value = "endDate", required = true) String endDate) {

        try {
            // Verify car exists
            Car car = rentalService.getCar(plateNumber);
        } catch (CarNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // For now, just log the application details and acknowledge reception.
        System.out.println("Received rental application for plate=" + plateNumber + ", firstName=" + firstName + ", lastName=" + lastName + ", email=" + email + ", begin=" + beginDate + ", end=" + endDate);

        // TODO: map these params to a domain object and create a RentalAgreement or forward to the agreement service.
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/ws/broadcast")
    public ResponseEntity<Void> broadcastAvailablePlates() {
        try {
            rentalService.broadcastAvailableCars();
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}

