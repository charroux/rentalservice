package com.charroux.auctionServiceServer;

import com.charroux.auction.*;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

@GrpcService
public class AuctionServiceImpl extends AuctionServiceGrpc.AuctionServiceImplBase {

    private final Logger logger = LoggerFactory.getLogger(AuctionServiceImpl.class);
    
    // Configuration métier (réduit à 5s pour debug)
    private static final int AUCTION_DURATION_SECONDS = 5;
    private static final int MIN_BID_INCREMENT = 10; // euros
    
    // Données en mémoire pour le prototype
    private final Map<String, CarModel> carModels = new ConcurrentHashMap<>(); // carModelId -> CarModel
    private final Map<String, AuctionSession> activeAuctions = new ConcurrentHashMap<>(); // carModelId -> AuctionSession
    private final Map<String, Set<String>> participantsByModel = new ConcurrentHashMap<>(); // carModelId -> Set<companyId>
    
    // Executor pour gérer les timeouts d'enchères
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    // Compteur pour générer des plaques d'immatriculation
    private int plateCounter = 1;
    
    // Classe interne pour gérer une session d'enchères
    private static class AuctionSession {
        final String carModelId;
        final long startTime;
        final int lowestPrice;
        final int highestPrice;
        String currentWinner;
        int currentBid;
        final List<StreamObserver<BidResponse>> observers = new ArrayList<>();
        
        AuctionSession(String carModelId, int lowestPrice, int highestPrice) {
            this.carModelId = carModelId;
            this.startTime = System.currentTimeMillis();
            this.lowestPrice = lowestPrice;
            this.highestPrice = highestPrice;
            this.currentBid = lowestPrice;
        }
        
        int getRemainingSeconds() {
            long elapsed = System.currentTimeMillis() - startTime;
            return Math.max(0, AUCTION_DURATION_SECONDS - (int)(elapsed / 1000));
        }
        
        boolean isActive() {
            return getRemainingSeconds() > 0 && currentBid < highestPrice;
        }
    }
    
    // Initialisation des modèles de voitures
    public AuctionServiceImpl() {
        initializeCarModels();
    }
    
    private void initializeCarModels() {
        CarModel ferrari = CarModel.newBuilder()
            .setBrand("Ferrari")
            .setModel("F8")
            .setLowestPrice(800)
            .setHighestPrice(1200)
            .build();
        carModels.put("Ferrari:F8", ferrari);
        
        CarModel porsche = CarModel.newBuilder()
            .setBrand("Porsche")
            .setModel("911")
            .setLowestPrice(600)
            .setHighestPrice(900)
            .build();
        carModels.put("Porsche:911", porsche);
        
        CarModel tesla = CarModel.newBuilder()
            .setBrand("Tesla")
            .setModel("Model S")
            .setLowestPrice(300)
            .setHighestPrice(500)
            .build();
        carModels.put("Tesla:Model S", tesla);
    }
    
    @Override
    public StreamObserver<Bidding> carAuction(StreamObserver<BidResponse> responseObserver) {
        logger.info("Nouvelle connexion d'enchères établie");
        
        return new StreamObserver<Bidding>() {
            @Override
            public void onNext(Bidding bidding) {
                handleBidding(bidding, responseObserver);
            }
            
            @Override
            public void onError(Throwable t) {
                logger.error("Erreur dans le stream d'enchères: {}", t.getMessage());
            }
            
            @Override
            public void onCompleted() {
                logger.info("Stream d'enchères fermé par le client");
            }
        };
    }
    
    private void handleBidding(Bidding bidding, StreamObserver<BidResponse> responseObserver) {
        String carModelId = bidding.getCarModelId();
        String companyId = bidding.getCarRentalCompanyId();
        int bidAmount = (int) bidding.getBidAmount();
        
        logger.info("Enchère reçue: {} pour {} avec montant {}", companyId, carModelId, bidAmount);
        
        // Vérifier si le modèle de voiture existe
        CarModel carModel = carModels.get(carModelId);
        if (carModel == null) {
            sendErrorResponse(responseObserver, "Modèle de voiture non trouvé: " + carModelId);
            return;
        }
        
        // Vérifier si l'entreprise a déjà participé à cette enchère
        Set<String> participants = participantsByModel.computeIfAbsent(carModelId, k -> new HashSet<>());
        if (participants.contains(companyId)) {
            sendErrorResponse(responseObserver, "L'entreprise " + companyId + " a déjà participé à cette enchère");
            return;
        }
        
        // Obtenir ou créer la session d'enchères
        AuctionSession session = activeAuctions.computeIfAbsent(carModelId, 
            k -> new AuctionSession(carModelId, carModel.getLowestPrice(), carModel.getHighestPrice()));
            
        // Ajouter l'observateur à la session
        session.observers.add(responseObserver);
        
        // Vérifier si l'enchère est encore active
        if (!session.isActive()) {
            endAuction(session);
            return;
        }
        
        // Valider l'enchère (sauf si c'est la première enchère au lowestPrice)
        boolean isFirstBid = session.currentWinner == null && bidAmount == session.lowestPrice;
        if (!isFirstBid && bidAmount < session.currentBid + MIN_BID_INCREMENT) {
            BidResponse response = BidResponse.newBuilder()
                .setWinning(false)
                .setCurrentHighestBid(session.currentBid)
                .setCurrentHighestBidder(session.currentWinner != null ? session.currentWinner : "Aucun")
                .setRemainingSeconds(session.getRemainingSeconds())
                .setStatus(AuctionStatus.ACTIVE)
                .build();
            responseObserver.onNext(response);
            return;
        }
        
        // Enchère valide - mettre à jour la session
        session.currentBid = bidAmount;
        session.currentWinner = companyId;
        participants.add(companyId);
        
        // Programmer la fin de l'enchère si c'est la première enchère
        if (participants.size() == 1) {
            logger.info("Première enchère détectée, programmation de la fin dans {}s", AUCTION_DURATION_SECONDS);
            scheduleAuctionEnd(session);
        }
        
        // Si on atteint le prix maximum, terminer l'enchère immédiatement
        if (bidAmount >= carModel.getHighestPrice()) {
            session.currentBid = carModel.getHighestPrice();
            endAuction(session);
        } else {
            // Notifier tous les participants
            broadcastAuctionUpdate(session);
        }
    }
    
    private void scheduleAuctionEnd(AuctionSession session) {
        logger.info("Programmation de la fin d'enchère pour {} dans {}s", session.carModelId, AUCTION_DURATION_SECONDS);
        
        scheduler.schedule(() -> {
            logger.info("Timeout d'enchère atteint pour {}, fin d'enchère", session.carModelId);
            if (activeAuctions.containsKey(session.carModelId)) {
                endAuction(session);
            }
        }, AUCTION_DURATION_SECONDS, TimeUnit.SECONDS);
    }
    
    private void broadcastAuctionUpdate(AuctionSession session) {
        BidResponse response = BidResponse.newBuilder()
            .setWinning(false)
            .setCurrentHighestBid(session.currentBid)
            .setCurrentHighestBidder(session.currentWinner != null ? session.currentWinner : "Aucun")
            .setRemainingSeconds(session.getRemainingSeconds())
            .setStatus(AuctionStatus.ACTIVE)
            .build();
            
        session.observers.forEach(observer -> {
            try {
                observer.onNext(response);
            } catch (Exception e) {
                logger.warn("Erreur lors de l'envoi de mise à jour: {}", e.getMessage());
            }
        });
    }
    
    private void endAuction(AuctionSession session) {
        logger.info("=== DEBUT FIN DE L'ENCHERE ===");
        logger.info("Fin de l'enchère pour {}: gagnant={}, prix={}, observers={}", 
                   session.carModelId, session.currentWinner, session.currentBid, session.observers.size());
        
        // Générer une plaque d'immatriculation
        String plateNumber = generatePlateNumber(session.carModelId);
        
        // Déterminer le statut final
        AuctionStatus finalStatus;
        int finalPrice;
        
        if (session.currentWinner == null) {
            // Aucune enchère - attribuer au prix le plus bas
            finalStatus = AuctionStatus.NO_BIDS;
            finalPrice = session.lowestPrice;
        } else if (session.currentBid >= session.highestPrice) {
            finalStatus = AuctionStatus.ENDED_BY_MAX_PRICE;
            finalPrice = session.highestPrice;
        } else {
            finalStatus = AuctionStatus.ENDED_BY_TIME;
            finalPrice = session.currentBid;
        }
        
        // Envoyer les réponses finales - tous les participants reçoivent une voiture (logique simplifiée)
        // Dans la réalité, seul le gagnant devrait recevoir la voiture
        session.observers.forEach(observer -> {
            try {
                BidResponse finalResponse = BidResponse.newBuilder()
                    .setWinning(true) // Simplification: tous gagnent une voiture (stock illimité pour prototype)
                    .setPlateNumber(plateNumber)
                    .setFinalPrice(finalPrice)
                    .setStatus(finalStatus)
                    .setRemainingSeconds(0)
                    .build();
                    
                observer.onNext(finalResponse);
                observer.onCompleted();
            } catch (Exception e) {
                logger.warn("Erreur lors de l'envoi de la réponse finale: {}", e.getMessage());
            }
        });
        
        // Nettoyer les données de l'enchère
        activeAuctions.remove(session.carModelId);
        participantsByModel.remove(session.carModelId);
        
        logger.info("=== FIN DE L'ENCHERE TERMINEE ===");
    }
    
    private String generatePlateNumber(String carModelId) {
        // Génération simple de plaque basée sur le modèle
        String[] parts = carModelId.split(":");
        String prefix = parts[0].substring(0, 2).toUpperCase();
        return String.format("%s-%03d-%s", prefix, plateCounter++, 
                            parts[1].substring(0, Math.min(2, parts[1].length())).toUpperCase());
    }
    
    private void sendErrorResponse(StreamObserver<BidResponse> responseObserver, String message) {
        logger.warn("Erreur d'enchère: {}", message);
        BidResponse errorResponse = BidResponse.newBuilder()
            .setWinning(false)
            .setStatus(AuctionStatus.ACTIVE) // ou un statut d'erreur si on en ajoute un
            .build();
        responseObserver.onNext(errorResponse);
    }
        
    @Override
    public void carModels(com.google.protobuf.Empty request, StreamObserver<CarModelsToBeRented> responseObserver) {
        logger.info("Demande de récupération des modèles de voitures");
        
        // Return a small static list of cars available to be rented with price ranges
        CarModel c1 = CarModel.newBuilder()
            .setBrand("Ferrari")
            .setModel("F8")
            .setLowestPrice(800)  // 800€/jour
            .setHighestPrice(1200) // 1200€/jour
            .build();
        CarModel c2 = CarModel.newBuilder()
            .setBrand("Porsche")
            .setModel("911")
            .setLowestPrice(600)  // 600€/jour
            .setHighestPrice(900)  // 900€/jour
            .build();
        CarModel c3 = CarModel.newBuilder()
            .setBrand("Tesla")
            .setModel("Model S")
            .setLowestPrice(300)  // 300€/jour
            .setHighestPrice(500)  // 500€/jour
            .build();

        CarModelsToBeRented list = CarModelsToBeRented.newBuilder()
                .addCars(c1)
                .addCars(c2)
                .addCars(c3)
                .build();

        responseObserver.onNext(list);
        responseObserver.onCompleted();
    }
}