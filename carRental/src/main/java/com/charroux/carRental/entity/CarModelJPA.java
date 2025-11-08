package com.charroux.carRental.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Collection;

@Entity
public class CarModelJPA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String brand;
    private String model;
    private Integer lowestPrice;   // Prix le plus bas en €/jour
    private Integer highestPrice;  // Prix le plus élevé en €/jour

    @OneToMany(mappedBy = "carModel", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Collection<Car> cars = new ArrayList<>();

    // Relation OneToMany vers BiddingJPA (un modèle peut avoir plusieurs enchères)
    @OneToMany(mappedBy = "carModelJPA", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Collection<BiddingJPA> biddings = new ArrayList<>();

    public CarModelJPA() {
    }

    public CarModelJPA(String brand, String model) {
        this.brand = brand;
        this.model = model;
    }

    public CarModelJPA(String brand, String model, Integer lowestPrice, Integer highestPrice) {
        this.brand = brand;
        this.model = model;
        this.lowestPrice = lowestPrice;
        this.highestPrice = highestPrice;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getLowestPrice() {
        return lowestPrice;
    }

    public void setLowestPrice(Integer lowestPrice) {
        this.lowestPrice = lowestPrice;
    }

    public Integer getHighestPrice() {
        return highestPrice;
    }

    public void setHighestPrice(Integer highestPrice) {
        this.highestPrice = highestPrice;
    }

    public Collection<Car> getCars() {
        return cars;
    }

    public void setCars(Collection<Car> cars) {
        this.cars = cars;
    }

    public Collection<BiddingJPA> getBiddings() {
        return biddings;
    }

    public void setBiddings(Collection<BiddingJPA> biddings) {
        this.biddings = biddings;
    }

    public void addCar(Car car) {
        cars.add(car);
        car.setCarModel(this);
    }

    public void removeCar(Car car) {
        cars.remove(car);
        car.setCarModel(null);
    }

    public void addBidding(BiddingJPA bidding) {
        biddings.add(bidding);
        bidding.setCarModelJPA(this);
    }

    public void removeBidding(BiddingJPA bidding) {
        biddings.remove(bidding);
        bidding.setCarModelJPA(null);
    }

    @Override
    public String toString() {
        return "CarModelJPA{" +
                "id=" + id +
                ", brand='" + brand + '\'' +
                ", model='" + model + '\'' +
                ", lowestPrice=" + lowestPrice +
                ", highestPrice=" + highestPrice +
                ", carsCount=" + cars.size() +
                ", biddingsCount=" + biddings.size() +
                '}';
    }
}
