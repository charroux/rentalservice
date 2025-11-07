package com.charroux.auctionServiceServer;

import com.charroux.auction.Auction;
import com.charroux.auction.AuctionServiceGrpc;
import com.charroux.auction.CreditApplication;
import com.charroux.auction.Bidding;
import com.charroux.auction.BidResponse;
import com.charroux.auction.CarModel;
import com.charroux.auction.CarToBeRented;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class AuctionServiceImpl extends AuctionServiceGrpc.AuctionServiceImplBase {

    Logger logger = LoggerFactory.getLogger(AuctionServiceImpl.class);

    @Override
    public StreamObserver<CreditApplication> rentCars(StreamObserver<Auction> responseObserver) {

        return new StreamObserver<CreditApplication>() {
            @Override
            public void onNext(CreditApplication creditApplication) {
                try{
                    logger.info("Credit application for " + creditApplication.getFirstName() + " " + creditApplication.getLastName() + " for " + creditApplication.getPrice() + " Euros.");
                    if(creditApplication.getPrice() > 1000){
                        throw new CreditException();
                    }
                    String customerLastName = creditApplication.getLastName();
                    if(customerLastName == null){
                        throw new CustomerNotFoundException();
                    }
                    responseObserver.onNext(Auction.newBuilder().setCreditReservedEvent("CREDIT_RESERVED").build());
                } catch (CreditException e) {
                    logger.info("Application rejected for the reason: not enough credit.");
                    responseObserver.onNext(Auction.newBuilder().setCreditReservedEvent("CREDIT_REJECTED").build());
                } catch (CustomerNotFoundException e) {
                    logger.info("Application rejected for the reason: user unknown.");
                    responseObserver.onNext(Auction.newBuilder().setCreditReservedEvent("CREDIT_REJECTED").build());
                }
            }

            @Override
            public void onError(Throwable t) {
                logger.info("Credit application failed for the reason: " + t);
            }

            @Override
            public void onCompleted() {
                logger.info("Credit application complete successfully.");
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<Bidding> carAuction(StreamObserver<BidResponse> responseObserver) {
        return new StreamObserver<Bidding>() {
            // Track the highest bid seen in this stream
            private int highest = 0;

            @Override
            public void onNext(Bidding bidding) {
                try {
                    int amount = bidding.getAmount();
                    CarModel car = bidding.hasCar() ? bidding.getCar() : null;
                    // New: Bidder info (added in proto). Safely read if present and log bidder id.
                    Integer bidderId = null;
                    if (bidding.hasBidder()) {
                        try {
                            bidderId = bidding.getBidder().getId();
                        } catch (Exception ex) {
                            logger.warn("Failed to read bidder id: {}", ex.getMessage());
                        }
                    }
                    logger.info("Received bid for car " + (car != null ? car.getBrand() + " " + car.getModel() : "<unknown>") + ", amount=" + amount + (bidderId != null ? ", bidderId=" + bidderId : ""));
                    boolean awarded = false;
                    if (amount > highest) {
                        highest = amount;
                        awarded = true;
                    }
            BidResponse.Builder respBuilder = BidResponse.newBuilder()
                .setAwarded(awarded)
                .setLatestBidding(amount);
            // Include plateNumber in the response (proto updated)
            if (car != null) {
            respBuilder.setPlateNumber(car.getModel());
            }
            BidResponse resp = respBuilder.build();
                    responseObserver.onNext(resp);
                } catch (Exception e) {
                    logger.error("Error processing bid: {}", e.getMessage(), e);
                }
            }

            @Override
            public void onError(Throwable t) {
                logger.warn("Car auction stream error: {}", t.getMessage());
            }

            @Override
            public void onCompleted() {
                logger.info("Car auction stream completed. Highest bid={}", highest);
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public void carsToBeRented(com.google.protobuf.Empty request, StreamObserver<CarToBeRented> responseObserver) {
        // Return a small static list of cars available to be rented
    // Add a plateNumber at initialization so clients can use it directly
    CarModel c1 = CarModel.newBuilder()
        .setBrand("Ferrari")
        .setModel("F8")
        .setPlateNumber("AA11BB")
        .build();
    CarModel c2 = CarModel.newBuilder()
        .setBrand("Porsche")
        .setModel("911")
        .setPlateNumber("BB22CC")
        .build();
    CarModel c3 = CarModel.newBuilder()
        .setBrand("Tesla")
        .setModel("Model S")
        .setPlateNumber("CC33DD")
        .build();

        CarToBeRented list = CarToBeRented.newBuilder()
                .addCars(c1)
                .addCars(c2)
                .addCars(c3)
                .build();

        responseObserver.onNext(list);
        responseObserver.onCompleted();
    }
}
