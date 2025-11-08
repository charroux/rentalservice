package com.charroux.carRental.service;

import com.charroux.carRental.entity.CarModelJPA;
import com.charroux.carRental.entity.CarModelJPARepository;
import com.charroux.auction.AuctionServiceGrpc;
import com.charroux.auction.CarModel;
import com.charroux.auction.CarModelsToBeRented;
import com.google.protobuf.Empty;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RentalServiceImpl implements RentalService {

    CarModelJPARepository carModelJPARepository;

    @GrpcClient("auctionService")
    private AuctionServiceGrpc.AuctionServiceBlockingStub auctionServiceStub;

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
                        CarModelJPA carModelJPA = new CarModelJPA(carModel.getBrand(), carModel.getModel());
                        carModelJPAList.add(carModelJPA);
                        logger.info("Nouveau modèle à sauvegarder : {} {}", carModel.getBrand(), carModel.getModel());
                    } else {
                        logger.info("Modèle déjà existant : {} {}", carModel.getBrand(), carModel.getModel());
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

}
