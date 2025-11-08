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
    public List<CarModelJPA> getListOfCars(){
        logger.info("Fetching list of cars to be rented");      
        return rentalService.carsToBeRented();
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

