package com.charroux.carRental.service;

import com.charroux.carRental.entity.Car;
import com.charroux.carRental.entity.CarModelJPA;
import com.charroux.carRental.entity.CarModelJPARepository;
import com.charroux.carRental.entity.CarRepository;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

/**
 * Service temporaire pour les tests sans dépendance gRPC
 */
@Service
@Primary
public class MockRentalServiceImpl implements RentalService {

    @Autowired
    private CarModelJPARepository carModelJPARepository;
    
    @Autowired
    private CarRepository carRepository;

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(MockRentalServiceImpl.class);
    private static final Random random = new Random();

    @Override
    public List<CarModelJPA> carsToBeRented() {
        return (List<CarModelJPA>) carModelJPARepository.findAll();
    }

    @Override
    public List<Car> getAvailableCars() {
        return (List<Car>) carRepository.findAll();
    }

    @Override
    public Car getCarByPlateNumber(String plateNumber) {
        return carRepository.findAll().iterator().next(); // Simple mock
    }

    @Override
    public Car participateInAuction(String brand, String model, String companyId) {
        logger.info("=== MOCK: Participation à l'enchère pour {} {} ===", brand, model);
        
        try {
            // Trouver le modèle de voiture par brand et model
            CarModelJPA carModel = ((List<CarModelJPA>) carModelJPARepository.findAll()).stream()
                .filter(cm -> brand.equals(cm.getBrand()) && model.equals(cm.getModel()))
                .findFirst()
                .orElse(null);
                
            if (carModel == null) {
                logger.error("Modèle de voiture non trouvé: {} {}", brand, model);
                return null;
            }

            // Simuler le résultat de l'enchère
            int baseRentalPrice = carModel.getHighestPrice(); // Prix de base pour la location
            int auctionPrice = simulateAuctionResult(carModel); // Prix d'acquisition simulé
            String plateNumber = generatePlateNumber();
            
            logger.info("Enchère simulée - Modèle: {} {}, Prix d'acquisition: {}€, Prix de base location: {}€", 
                       carModel.getBrand(), carModel.getModel(), auctionPrice, baseRentalPrice);

            // Créer l'entité Car
            Car resultCar = new Car();
            resultCar.setPlateNumber(plateNumber);
            resultCar.setCarModel(carModel);
            resultCar.setFinalPrice(auctionPrice); // Prix d'acquisition
            resultCar.setRentalPrice(baseRentalPrice); // Prix de base pour la location
            
            // Calculer la marge et appliquer une remise si possible
            int margin = baseRentalPrice - auctionPrice;
            int finalCustomerPrice = baseRentalPrice;
            
            logger.info("Marge calculée: {}€ (Prix location {} - Prix acquisition {})", 
                       margin, baseRentalPrice, auctionPrice);
            
            if (margin > 0) {
                // Appliquer une remise de 5% du prix de base, limitée par la marge
                double discountPercentage = 0.05;
                int discountAmount = (int) Math.round(baseRentalPrice * discountPercentage);
                discountAmount = Math.min(discountAmount, margin);
                
                if (discountAmount > 0) {
                    finalCustomerPrice = baseRentalPrice - discountAmount;
                    logger.info("Remise appliquée: {}€ sur {}€ (marge disponible: {}€)", 
                               discountAmount, baseRentalPrice, margin);
                }
            }
            
            resultCar.setFinalCustomerPrice(finalCustomerPrice);
            
            // Sauvegarder l'entité Car en base
            resultCar = carRepository.save(resultCar);
            
            logger.info("Voiture sauvegardée - Plaque: {}, Prix acquisition: {}€, Prix base: {}€, Prix final: {}€, Marge: {}€", 
                       resultCar.getPlateNumber(), resultCar.getFinalPrice(), 
                       resultCar.getRentalPrice(), resultCar.getFinalCustomerPrice(), resultCar.getMargin());
            
            return resultCar;
            
        } catch (Exception e) {
            logger.error("Erreur lors de la simulation d'enchère: {}", e.getMessage(), e);
            return null;
        }
    }
    
    private int simulateAuctionResult(CarModelJPA carModel) {
        // Simuler un prix d'enchère entre lowestPrice et highestPrice - 20%
        int min = carModel.getLowestPrice();
        int max = (int) (carModel.getHighestPrice() * 0.8); // Laisse de la marge pour la remise
        return (int) Math.round(min + (max - min) * random.nextDouble());
    }
    
    private String generatePlateNumber() {
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String numbers = "0123456789";
        
        StringBuilder plate = new StringBuilder();
        // Format: ABC-123
        for (int i = 0; i < 3; i++) {
            plate.append(letters.charAt(random.nextInt(letters.length())));
        }
        plate.append("-");
        for (int i = 0; i < 3; i++) {
            plate.append(numbers.charAt(random.nextInt(numbers.length())));
        }
        
        return plate.toString();
    }
}