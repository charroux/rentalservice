package com.charroux.carRental.service;

import com.charroux.carRental.entity.Car;
import com.charroux.carRental.entity.CarRepository;
import com.charroux.carRental.entity.RentalAgreement;
import com.charroux.carRental.entity.RentalAgreementRepository;
import com.charroux.carRental.web.CarNotFoundException;
import com.charroux.auction.Auction;
import com.charroux.auction.CreditApplication;
import com.charroux.auction.CarToBeRented;
import com.charroux.auction.AuctionServiceGrpc;
import com.google.protobuf.Empty;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import org.slf4j.Logger;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CountDownLatch;

@Service
public class RentalServiceImpl implements RentalService {

    @Value("${CUSTOMER.SERVICE.HOST}")
    String customerHost;

    @Value("${CUSTOMER.SERVICE.PORT}")
    String customerPost;

    CarRepository carRepository;
    RentalAgreementRepository rentalAgreementRepository;
    private final SimpMessagingTemplate messagingTemplate;

    Logger logger = org.slf4j.LoggerFactory.getLogger(RentalServiceImpl.class);

    @Autowired
    public RentalServiceImpl(CarRepository carRepository,
                             RentalAgreementRepository rentalAgreementRepository,
                             @Value("${CUSTOMER.SERVICE.HOST}") String customerHost,
                             @Value("${CUSTOMER.SERVICE.PORT}") String customerPost,
                             SimpMessagingTemplate messagingTemplate) {
        super();
        this.carRepository = carRepository;
        this.rentalAgreementRepository = rentalAgreementRepository;
        this.messagingTemplate = messagingTemplate;
        // assign injected host/port values
        this.customerHost = customerHost;
        this.customerPost = customerPost;

        // On construction, fetch list of cars available from AuctionService and persist them
        // Retry with exponential backoff if service is not ready
        int maxRetries = 5;
        int retryDelayMs = 1000; // Start with 1 second
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                logger.info("Attempting to connect to AuctionService (attempt {}/{})", attempt, maxRetries);
                final ManagedChannel channel = ManagedChannelBuilder.forTarget(this.customerHost + ":" + this.customerPost)
                        .usePlaintext()
                        .build();
                AuctionServiceGrpc.AuctionServiceBlockingStub blockingStub = AuctionServiceGrpc.newBlockingStub(channel);
                CarToBeRented available = blockingStub.carsToBeRented(Empty.getDefaultInstance());
                if (available != null) {
                    for (com.charroux.auction.CarModel protoCar : available.getCarsList()) {
                        // Map proto fields to JPA entity. Use `model` as plateNumber if present.
                        String model = protoCar.getModel();
                        String brand = protoCar.getBrand();
                        String plate = protoCar.getPlateNumber();
                        // Avoid inserting duplicates by plate number
                        boolean exists = !carRepository.findByPlateNumber(plate).isEmpty();
                        if (!exists) {
                            com.charroux.carRental.entity.Car entity = new com.charroux.carRental.entity.Car(plate, brand, model, 0);
                            carRepository.save(entity);
                        }
                    }
                    logger.info("Successfully loaded {} cars from AuctionService", available.getCarsCount());
                }
                channel.shutdown();
                break; // Success - exit retry loop
            } catch (Exception e) {
                logger.warn("Failed to fetch cars from AuctionService (attempt {}/{}): {}", 
                           attempt, maxRetries, e.getMessage());
                
                if (attempt < maxRetries) {
                    try {
                        logger.info("Retrying in {} ms...", retryDelayMs);
                        Thread.sleep(retryDelayMs);
                        retryDelayMs *= 2; // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.error("Retry interrupted", ie);
                        break;
                    }
                } else {
                    logger.error("All retry attempts failed. Application will start without initial car data.");
                }
            }
        }
    }

    /**
     * Fetch available cars from AuctionService and push their plateNumbers to WebSocket topic /topic/plates
     */
    public void broadcastAvailableCars() {
        try {
            final ManagedChannel channel = ManagedChannelBuilder.forTarget(this.customerHost + ":" + this.customerPost)
                    .usePlaintext()
                    .build();
            AuctionServiceGrpc.AuctionServiceBlockingStub blockingStub = AuctionServiceGrpc.newBlockingStub(channel);
            CarToBeRented available = blockingStub.carsToBeRented(Empty.getDefaultInstance());
            if (available != null) {
                for (com.charroux.auction.CarModel protoCar : available.getCarsList()) {
                    String plate = protoCar.getPlateNumber();
                    if (plate == null) plate = protoCar.getModel();
                    logger.info("Broadcasting plateNumber via WebSocket: {}", plate);
                    try {
                        messagingTemplate.convertAndSend("/topic/plates", plate);
                    } catch (Exception mex) {
                        logger.error("Failed to send WS message: {}", mex.getMessage());
                    }
                }
            }
            channel.shutdown();
        } catch (Exception e) {
            logger.error("Failed to fetch/broadcast cars: {}", e.getMessage());
        }
    }

    class AuctionObserver extends Thread implements StreamObserver<Auction> {

        CountDownLatch countDownLatch;
        RentalAgreement rentalAgreement;

        public AuctionObserver(CountDownLatch countDownLatch, RentalAgreement rentalAgreement){
            this.countDownLatch = countDownLatch;
            this.rentalAgreement = rentalAgreement;
        }

        @Override
        public void onNext(Auction auction) {
            String creditReservedEvent = auction.getCreditReservedEvent();

            switch (creditReservedEvent) {
                case "CREDIT_RESERVED" :
                    rentalAgreement.setState(RentalAgreement.State.CREDIT_RESERVED);
                    break;
                default:
                    rentalAgreement.setState(RentalAgreement.State.CREDIT_REJECTED);
            }
            logger.info("onNext client receives: " + auction);
        }

        @Override
        public void onError(Throwable t) {

        }

        @Override
        public void onCompleted() {
            logger.info("on completed");
            countDownLatch.countDown();
        }

        @Override
        public void run() {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            logger.info("fin run");
        }
    }

    @Override
    public RentalAgreement rent(long customerId, int numberOfCars) throws CarNotFoundException {

        List<Car> cars = this.carsToBeRented();

        if(cars.size() < numberOfCars){
            throw new CarNotFoundException();
        }

        /*String host = System.getenv("ECHO_SERVICE_HOST");
        String port = System.getenv("ECHO_SERVICE_PORT");

        System.out.println("host1: " + host);
        System.out.println("port:" + port);
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(host + ":" + port)
                .usePlaintext()
                .build();*/

        final ManagedChannel channel = ManagedChannelBuilder.forTarget(customerHost + ":" + customerPost)
                .usePlaintext()
                .build();

        AuctionServiceGrpc.AuctionServiceStub nonBlockingStub = AuctionServiceGrpc.newStub(channel);

        CountDownLatch countDownLatch = new CountDownLatch(1);

        RentalAgreement rentalAgreement = new RentalAgreement(customerId, RentalAgreement.State.PENDING);
        rentalAgreement.setState(RentalAgreement.State.PENDING);
        rentalAgreementRepository.save(rentalAgreement);

        logger.info("debut=" + rentalAgreement.hashCode() + " " + rentalAgreement);

        AuctionObserver auctionObserver = new AuctionObserver(countDownLatch, rentalAgreement);

        auctionObserver.start();

        StreamObserver<CreditApplication> carsObserver = nonBlockingStub.rentCars(auctionObserver);

        Car car;
        for (int i=0; i<numberOfCars; i++) {
            car = cars.get(i);
            rentalAgreement.addCar(car);
            logger.info(car.toString());
            CreditApplication creditApplication = CreditApplication.newBuilder().setEmail("me@gmail.com").setPrice(car.getPrice()).build();
            carsObserver.onNext(creditApplication);
        }

        carsObserver.onCompleted();

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        channel.shutdown();

        if(rentalAgreement.getState() == RentalAgreement.State.CREDIT_REJECTED) {
            System.out.println("rejet");
            rentalAgreement.getCars().stream().peek(aCar -> aCar.setRentalAgreement(null));
            rentalAgreement.setCars(new ArrayList<Car>());
        } else {
            System.out.println("pas rejet");
        }

        System.out.println("fin=" + rentalAgreement.hashCode() + " " + rentalAgreement);

        rentalAgreementRepository.save(rentalAgreement);

        System.out.println("it1:");
        Iterator<RentalAgreement> iterator = rentalAgreementRepository.findAll().iterator();
        while ((iterator.hasNext())){
            System.out.println(iterator.next());
        }

        System.out.println("it2:");
        Iterator<RentalAgreement> iterator1 = this.getAgreements().iterator();
        while ((iterator1.hasNext())){
            System.out.println(iterator1.next());
        }

        return rentalAgreement;
    }

    @Override
    public List<Car> carsToBeRented() {
        List<Car> cars = new ArrayList<>();
        carRepository.findAll().forEach(cars::add);
        return cars;
    }

    @Override
    public Car getCar(String plateNumber) throws CarNotFoundException {
        List<Car> cars = carRepository.findByPlateNumber(plateNumber);
        if(cars.size() == 0) throw  new CarNotFoundException();
        return cars.get(0);
    }

    @Override
    public List<RentalAgreement> getAgreements(){
        List<RentalAgreement> result = new ArrayList<RentalAgreement>();
        Iterator<RentalAgreement> it = rentalAgreementRepository.findAll().iterator();
        while (it.hasNext()) {
            result.add(it.next());
        }
        return result;
    }

    @Override
    public RentalAgreement getAgreement(long customerId) {
        List<RentalAgreement> agreements = rentalAgreementRepository.findByCustomerId(customerId);
        RentalAgreement rentalAgreement = agreements.get(0);
        return rentalAgreement;
    }

    @Override
    public Collection<Car> getCars(RentalAgreement agreement) {
        Optional<RentalAgreement> optional = rentalAgreementRepository.findById(agreement.getId());
        if(optional.isEmpty()) return new ArrayList<Car>();
        return optional.get().getCars();
    }

}
