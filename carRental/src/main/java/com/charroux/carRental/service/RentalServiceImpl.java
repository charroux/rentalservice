package com.charroux.carRental.service;

import com.charroux.carRental.entity.CarModelJPA;
import com.charroux.carRental.entity.CarModelJPARepository;
import com.charroux.auction.*;
import com.google.protobuf.Empty;
import net.devh.boot.grpc.client.inject.GrpcClient;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.Objects;

@Service
public class RentalServiceImpl implements RentalService {

    CarModelJPARepository carModelJPARepository;

    @GrpcClient("auctionService")
    private AuctionServiceGrpc.AuctionServiceBlockingStub auctionServiceStub;
    
    @GrpcClient("auctionService")
    private AuctionServiceGrpc.AuctionServiceStub auctionServiceAsyncStub;

    Logger logger = org.slf4j.LoggerFactory.getLogger(RentalServiceImpl.class);

    @Autowired
    public RentalServiceImpl(CarModelJPARepository carModelJPARepository) {
        super();
        this.carModelJPARepository = carModelJPARepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        loadCarModelsFromAuctionService();
    }

    private void loadCarModelsFromAuctionService() {
        int maxAttempts = 5;
        int attemptDelay = 2000; // 2 seconds
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                logger.info("Tentative {}/{} : Récupération des modèles de voitures depuis le service d'enchères...", attempt, maxAttempts);
                
                // Invoquer la méthode gRPC carModels
                CarModelsToBeRented carModelsToBeRented = auctionServiceStub.carModels(Empty.getDefaultInstance());
                
                logger.info("Succès ! Nombre de modèles récupérés : {}", carModelsToBeRented.getCarsCount());
                
                // Convertir les CarModel en CarModelJPA et sauvegarder
                List<CarModelJPA> carModelJPAList = new ArrayList<>();
                
                for (CarModel carModel : carModelsToBeRented.getCarsList()) {
                    // Vérifier si le modèle existe déjà en base pour éviter les doublons
                    CarModelJPA existingModel = findExistingCarModel(carModel.getBrand(), carModel.getModel());
                    
                    if (existingModel == null) {
                        CarModelJPA carModelJPA = new CarModelJPA(
                            carModel.getBrand(), 
                            carModel.getModel(),
                            (int) carModel.getLowestPrice(),
                            (int) carModel.getHighestPrice()
                        );
                        carModelJPAList.add(carModelJPA);
                        logger.info("Nouveau modèle à sauvegarder : {} {} (Prix: {}€-{}€/jour)", 
                            carModel.getBrand(), carModel.getModel(), 
                            carModel.getLowestPrice(), carModel.getHighestPrice());
                    } else {
                        // Mettre à jour les prix si nécessaire
                        boolean updated = false;
                        if (!Objects.equals(existingModel.getLowestPrice(), (int) carModel.getLowestPrice())) {
                            existingModel.setLowestPrice((int) carModel.getLowestPrice());
                            updated = true;
                        }
                        if (!Objects.equals(existingModel.getHighestPrice(), (int) carModel.getHighestPrice())) {
                            existingModel.setHighestPrice((int) carModel.getHighestPrice());
                            updated = true;
                        }
                        if (updated) {
                            carModelJPARepository.save(existingModel);
                            logger.info("Modèle mis à jour : {} {} (Prix: {}€-{}€/jour)", 
                                carModel.getBrand(), carModel.getModel(), 
                                carModel.getLowestPrice(), carModel.getHighestPrice());
                        } else {
                            logger.info("Modèle déjà existant sans changement : {} {}", carModel.getBrand(), carModel.getModel());
                        }
                    }
                }
                
                // Sauvegarder tous les nouveaux CarModelJPA
                if (!carModelJPAList.isEmpty()) {
                    carModelJPARepository.saveAll(carModelJPAList);
                    logger.info("Sauvegarde terminée : {} modèles sauvegardés", carModelJPAList.size());
                } else {
                    logger.info("Aucun nouveau modèle à sauvegarder");
                }
                
                return; // Success, exit retry loop
                
            } catch (Exception e) {
                logger.warn("Tentative {}/{} échouée : {}", attempt, maxAttempts, e.getMessage());
                
                if (attempt == maxAttempts) {
                    logger.error("Échec définitif après {} tentatives. Dernière erreur : {}", maxAttempts, e.getMessage(), e);
                } else {
                    logger.info("Nouvelle tentative dans {} ms...", attemptDelay);
                    try {
                        Thread.sleep(attemptDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.error("Interruption pendant l'attente entre les tentatives");
                        return;
                    }
                }
            }
        }
    }

    private CarModelJPA findExistingCarModel(String brand, String model) {
        // Recherche d'un modèle existant par marque et modèle
        return carModelJPARepository.findByBrandAndModel(brand, model);
    }

    @Override
    public List<CarModelJPA> carsToBeRented() {
        List<CarModelJPA> carModels = new ArrayList<>();
        carModelJPARepository.findAll().forEach(carModels::add);
        logger.info("Retour de {} modèles de voitures disponibles à la location", carModels.size());
        return carModels;
    }

    @Override
    public List<com.charroux.carRental.entity.Car> getAvailableCars() {
        List<com.charroux.carRental.entity.Car> availableCars = new ArrayList<>();
        List<CarModelJPA> carModels = carsToBeRented();
        
        // Pour chaque modèle de voiture, créer quelques instances de Car avec des plaques d'immatriculation
        // et initialiser le rentalPrice avec highestPrice
        int carCounter = 1;
        for (CarModelJPA carModel : carModels) {
            // Créer 2-3 voitures par modèle pour simuler un inventaire
            for (int i = 1; i <= 3; i++) {
                String plateNumber = String.format("%s-%03d-%s", 
                    carModel.getBrand().substring(0, 2).toUpperCase(),
                    carCounter++,
                    carModel.getModel().substring(0, Math.min(2, carModel.getModel().length())).toUpperCase());
                
                com.charroux.carRental.entity.Car car = new com.charroux.carRental.entity.Car(plateNumber, carModel.getHighestPrice());
                car.setCarModel(carModel);
                availableCars.add(car);
            }
        }
        
        logger.info("Retour de {} voitures disponibles à la location générées à partir de {} modèles", 
                    availableCars.size(), carModels.size());
        return availableCars;
    }

    @Override
    public com.charroux.carRental.entity.Car getCarByPlateNumber(String plateNumber) {
        List<com.charroux.carRental.entity.Car> allCars = getAvailableCars();
        return allCars.stream()
                .filter(car -> car.getPlateNumber().equalsIgnoreCase(plateNumber))
                .findFirst()
                .orElse(null);
    }

    @Override
    public com.charroux.carRental.entity.Car participateInAuction(String brand, String model, String companyId) {
        logger.info("Participation à l'enchère pour {} {} par l'entreprise {}", brand, model, companyId);
        
        // Rechercher le modèle en base de données
        CarModelJPA carModel = findExistingCarModel(brand, model);
        if (carModel == null) {
            logger.error("Modèle de voiture non trouvé: {} {}", brand, model);
            return null;
        }
        
        String carModelId = brand + ":" + model;
        
        try {
            // Utiliser CompletableFuture pour gérer l'asynchrone
            java.util.concurrent.CompletableFuture<com.charroux.carRental.entity.Car> future = new java.util.concurrent.CompletableFuture<>();
            
            // Créer le StreamObserver pour recevoir les réponses
            StreamObserver<BidResponse> responseObserver = new StreamObserver<BidResponse>() {
                @Override
                public void onNext(BidResponse response) {
                    logger.info("Réponse d'enchère reçue: winning={}, plateNumber={}, finalPrice={}, status={}", 
                               response.getWinning(), response.getPlateNumber(), response.getFinalPrice(), response.getStatus());
                    
                    // Si on reçoit une réponse finale (avec plaque d'immatriculation)
                    if (!response.getPlateNumber().isEmpty()) {
                        // Créer l'objet Car avec le résultat de l'enchère
                        com.charroux.carRental.entity.Car resultCar = new com.charroux.carRental.entity.Car(
                            response.getPlateNumber(), 
                            (int) response.getFinalPrice(),    // Prix d'acquisition (coût pour carRentalCompany)
                            carModel.getHighestPrice()        // Prix facturé à l'utilisateur final
                        );
                        resultCar.setCarModel(carModel);
                        
                        logger.info("Voiture créée - Plaque: {}, Prix d'acquisition: {}€, Prix de location: {}€, Marge: {}€", 
                                   resultCar.getPlateNumber(), resultCar.getFinalPrice(), 
                                   resultCar.getRentalPrice(), resultCar.getMargin());
                        
                        future.complete(resultCar);
                    }
                }
                
                @Override
                public void onError(Throwable t) {
                    logger.error("Erreur dans l'enchère: {}", t.getMessage());
                    future.completeExceptionally(t);
                }
                
                @Override
                public void onCompleted() {
                    logger.info("Stream d'enchères terminé");
                }
            };
            
            // Établir la connexion gRPC streaming
            StreamObserver<Bidding> requestObserver = auctionServiceAsyncStub.carAuction(responseObserver);
            
            // Envoyer l'enchère initiale avec le lowestPrice
            Bidding initialBid = Bidding.newBuilder()
                .setCarRentalCompanyId(companyId)
                .setCarModelId(carModelId)
                .setBidAmount(carModel.getLowestPrice())
                .setTimestamp(System.currentTimeMillis())
                .build();
                
            requestObserver.onNext(initialBid);
            
            // Attendre la réponse (avec timeout)
            return future.get(10, java.util.concurrent.TimeUnit.SECONDS); // 10s pour être sûr
            
        } catch (Exception e) {
            logger.error("Erreur lors de la participation à l'enchère: {}", e.getMessage(), e);
            return null;
        }
    }

}
