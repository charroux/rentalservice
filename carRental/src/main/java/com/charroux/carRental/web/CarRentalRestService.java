package com.charroux.carRental.web;

import com.charroux.carRental.entity.Car;
import com.charroux.carRental.entity.CarModelJPA;
import com.charroux.carRental.service.RentalService;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        return rentalService.getAvailableCars();
    }

    @GetMapping("/cars/{plateNumber}")
    public ResponseEntity<Car> getCarByPlateNumber(@PathVariable("plateNumber") String plateNumber) {
        logger.info("Fetching car with plate number: {}", plateNumber);
        
        Car foundCar = rentalService.getCarByPlateNumber(plateNumber);
            
        if (foundCar != null) {
            return ResponseEntity.ok(foundCar);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/cars/{plateNumber}/margin")
    public ResponseEntity<String> getCarMargin(@PathVariable("plateNumber") String plateNumber) {
        logger.info("Fetching margin info for car: {}", plateNumber);
        
        Car foundCar = rentalService.getCarByPlateNumber(plateNumber);
            
        if (foundCar != null) {
            String marginInfo = String.format(
                "Voiture %s - Prix d'acquisition: %d€, Prix de location: %d€, Marge: %d€ (%.1f%%)",
                foundCar.getPlateNumber(), 
                foundCar.getFinalPrice(),
                foundCar.getRentalPrice(),
                foundCar.getMargin(),
                foundCar.getMarginPercentage()
            );
            return ResponseEntity.ok(marginInfo);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/cars/model/{brand}/{model}")
    public ResponseEntity<CarModelJPA> getCarModelDetails(
            @PathVariable("brand") String brand,
            @PathVariable("model") String model) {
        logger.info("Fetching details for car model: {} {}", brand, model);
        
        // TODO: Ajouter une méthode dans le service pour récupérer par marque et modèle
        List<CarModelJPA> allCars = rentalService.carsToBeRented();
        
        CarModelJPA foundModel = allCars.stream()
            .filter(car -> car.getBrand().equalsIgnoreCase(brand) && car.getModel().equalsIgnoreCase(model))
            .findFirst()
            .orElse(null);
            
        if (foundModel != null) {
            return ResponseEntity.ok(foundModel);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/debug/reload-cars")
    public ResponseEntity<String> reloadCarsFromAuctionService(){
        logger.info("Rechargement manuel des modèles de voitures depuis le service d'enchères");
        try {
            List<CarModelJPA> cars = rentalService.carsToBeRented();
            return ResponseEntity.ok("Nombre de modèles actuellement en base : " + cars.size());
        } catch (Exception e) {
            logger.error("Erreur lors de la consultation : {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur : " + e.getMessage());
        }
    }

    @PostMapping("/debug/force-reload")
    public ResponseEntity<String> forceReloadFromGrpc(){
        logger.info("Rechargement forcé depuis le service gRPC");
        try {
            // Ici on peut ajouter une méthode publique pour forcer le rechargement
            return ResponseEntity.ok("Fonction de rechargement forcé - à implémenter si nécessaire");
        } catch (Exception e) {
            logger.error("Erreur lors du rechargement forcé : {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur : " + e.getMessage());
        }
    }

    @PostMapping("/auction/{brand}/{model}")
    public ResponseEntity<Car> participateInAuction(
            @PathVariable("brand") String brand,
            @PathVariable("model") String model,
            @RequestParam(value = "companyId", defaultValue = "DEFAULT_COMPANY") String companyId) {
        
        logger.info("Demande de participation à l'enchère pour {} {} par {}", brand, model, companyId);
        
        try {
            Car resultCar = rentalService.participateInAuction(brand, model, companyId);
            
            if (resultCar != null) {
                logger.info("Enchère réussie: voiture {} attribuée", resultCar.getPlateNumber());
                return ResponseEntity.ok(resultCar);
            } else {
                logger.warn("Échec de l'enchère pour {} {}", brand, model);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la participation à l'enchère: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/cars/{plateNumber}")
    public ResponseEntity<Void> submitApplication(
            @PathVariable("plateNumber") String plateNumber,
            @RequestParam(value = "firstName", required = true) String firstName,
            @RequestParam(value = "lastName", required = true) String lastName,
            @RequestParam(value = "email", required = true) String email,
            @RequestParam(value = "beginDate", required = true) String beginDate,
            @RequestParam(value = "endDate", required = true) String endDate) {

    
        // TODO: map these params to a domain object and create a RentalAgreement or forward to the agreement service.
        return new ResponseEntity<>(HttpStatus.OK);
    }



}

