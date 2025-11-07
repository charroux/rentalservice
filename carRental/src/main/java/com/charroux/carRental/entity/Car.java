package com.charroux.carRental.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
public class Car {

    String plateNumber;
    String brand;
    String model;
    int price;
    RentalAgreement rentalAgreement;
    long id;

    public Car() {
    }

    public Car(String plateNumber, String brand, String model, int price) {
        this.plateNumber = plateNumber;
        this.brand = brand;
        this.model = model;
        this.price = price;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
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

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    @ManyToOne
    @JsonIgnore
    public RentalAgreement getRentalAgreement() {
        return rentalAgreement;
    }

    public void setRentalAgreement(RentalAgreement rentalAgreement) {
        this.rentalAgreement = rentalAgreement;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Car{" +
                "plateNumber='" + plateNumber + '\'' +
                ", brand='" + brand + '\'' +
                ", model='" + model + '\'' +
                ", price=" + price +
                ", id=" + id +
                '}';
    }
}


