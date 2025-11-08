package com.charroux.auctionServiceServer;

import com.charroux.auction.AuctionServiceGrpc;
import com.charroux.auction.CarModel;
import com.charroux.auction.CarModelsToBeRented;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class AuctionServiceImpl extends AuctionServiceGrpc.AuctionServiceImplBase {

    private final Logger logger = LoggerFactory.getLogger(AuctionServiceImpl.class);
        
    @Override
    public void carModels(com.google.protobuf.Empty request, StreamObserver<CarModelsToBeRented> responseObserver) {
        // Return a small static list of cars available to be rented
        CarModel c1 = CarModel.newBuilder()
            .setBrand("Ferrari")
            .setModel("F8")
            .build();
        CarModel c2 = CarModel.newBuilder()
            .setBrand("Porsche")
            .setModel("911")
            .build();
        CarModel c3 = CarModel.newBuilder()
            .setBrand("Tesla")
            .setModel("Model S")
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