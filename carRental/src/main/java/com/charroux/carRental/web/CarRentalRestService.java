package com.charroux.carRental.web;

import com.charroux.carRental.dto.AuctionResultDTO;
import com.charroux.carRental.dto.OfferDTO;
import com.charroux.carRental.entity.Car;
import com.charroux.carRental.entity.CarModelJPA;
import com.charroux.carRental.entity.CarModelJPARepository;
import com.charroux.carRental.service.RentalService;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class CarRentalRestService {

    RentalService rentalService;
    CarModelJPARepository carModelJPARepository;
    Logger logger = org.slf4j.LoggerFactory.getLogger(CarRentalRestService.class);

    @Autowired
    public CarRentalRestService(RentalService rentalService, CarModelJPARepository carModelJPARepository) {
        super();
        this.rentalService = rentalService;
        this.carModelJPARepository = carModelJPARepository;
    }

    /**
     * LEGACY ENDPOINT - For backward compatibility
     * Returns raw CarModel entities
     */
    @GetMapping("/car-models")
    public List<CarModelJPA> getCarModels(){
        logger.info("Fetching list of car models available for auction");      
        return rentalService.carsToBeRented();
    }

    /**
     * MAIN OFFERS ENDPOINT - Used by Angular frontend
     * Returns formatted offers with pricing for display
     */
    @GetMapping("/offers")
    public List<OfferDTO> getOffers(){
        logger.info("Fetching list of rental offers for Angular frontend");
        return rentalService.carsToBeRented().stream()
            .map(carModel -> new OfferDTO(
                carModel.getId(),
                carModel.getBrand(),
                carModel.getModel(),
                generatePhotoUrl(carModel.getBrand(), carModel.getModel()),
                java.math.BigDecimal.valueOf(carModel.getHighestPrice())  // Using highestPrice as rentalPrice
            ))
            .collect(Collectors.toList());
    }

    private String generatePhotoUrl(String brand, String model) {
        // Generate a default photo URL based on brand and model
        return String.format("/assets/cars/%s-%s.jpg", 
            brand.toLowerCase().replace(" ", "-"),
            model.toLowerCase().replace(" ", "-"));
    }

    /**
     * LEGACY ENDPOINT - For backward compatibility
     * Returns list of available cars with plate numbers
     */
    @GetMapping("/cars")
    public List<Car> getListOfCars(){
        logger.info("Fetching list of cars to be rented");      
        return rentalService.getAvailableCars();
    }

    /**
     * LEGACY ENDPOINT - For backward compatibility
     * Get specific car by plate number
     */
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
    


    /**
     * FORM SUBMISSION ENDPOINT - Used by Angular for rental applications
     * Receives customer info and rental dates
     */
    @PostMapping("/cars/{plateNumber}")
    public ResponseEntity<Void> submitApplication(
            @PathVariable("plateNumber") String plateNumber,
            @RequestParam(value = "firstName", required = true) String firstName,
            @RequestParam(value = "lastName", required = true) String lastName,
            @RequestParam(value = "email", required = true) String email,
            @RequestParam(value = "beginDate", required = true) String beginDate,
            @RequestParam(value = "endDate", required = true) String endDate) {

        logger.info("Application received for car {} by {} {} ({})", plateNumber, firstName, lastName, email);
        // TODO: map these params to a domain object and create a RentalAgreement or forward to the agreement service.
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * MAIN AUCTION ENDPOINT - Used by Angular frontend
     * Participate in auction using carModelId
     */
    @PostMapping("/auction/participate")
    public ResponseEntity<AuctionResultDTO> participateInAuctionByCarModelId(@RequestBody CarModelIdRequest request) {
        logger.info("Demande de participation à l'enchère pour carModelId {}", request.getCarModelId());
        
        try {
            // Vérification et récupération du modèle de voiture
            if (request.getCarModelId() == null) {
                logger.warn("CarModelId est null dans la requête");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            
            @SuppressWarnings("null")
            CarModelJPA carModel = carModelJPARepository.findById(request.getCarModelId()).orElse(null);
            if (carModel == null) {
                logger.warn("Modèle de voiture non trouvé: {}", request.getCarModelId());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            
            // Déléguer à l'ancien service avec brand et model
            Car resultCar = rentalService.participateInAuction(carModel.getBrand(), carModel.getModel(), "DEFAULT_COMPANY");
            
            if (resultCar != null) {
                logger.info("Enchère réussie: voiture {} attribuée", resultCar.getPlateNumber());
                
                // Créer le DTO de réponse avec les informations de remise
                java.math.BigDecimal originalPrice = java.math.BigDecimal.valueOf(resultCar.getRentalPrice());
                java.math.BigDecimal finalPrice = java.math.BigDecimal.valueOf(resultCar.getFinalCustomerPrice());
                java.math.BigDecimal discountAmount = originalPrice.subtract(finalPrice);
                boolean discountApplied = discountAmount.compareTo(java.math.BigDecimal.ZERO) > 0;
                
                AuctionResultDTO result = new AuctionResultDTO(
                    resultCar.getPlateNumber(),
                    finalPrice,
                    originalPrice,
                    discountAmount,
                    discountApplied
                );
                
                return ResponseEntity.ok(result);
            } else {
                logger.warn("Échec de l'enchère pour carModelId {}", request.getCarModelId());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la participation à l'enchère: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Classe interne pour la requête
    public static class CarModelIdRequest {
        private Long carModelId;
        
        public Long getCarModelId() { return carModelId; }
        public void setCarModelId(Long carModelId) { this.carModelId = carModelId; }
    }

}

