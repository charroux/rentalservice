package com.charroux.carRental.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class RentalContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private LocalDateTime contractDate;
    private String status;

    // Relation ManyToOne vers Car
    // Un contrat concerne une seule voiture
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id")
    private Car car;

    // Relation ManyToOne vers CustomerJPA
    // Un contrat est associé à un seul client
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private CustomerJPA customer;

    // Constructeurs
    public RentalContract() {
        this.contractDate = LocalDateTime.now();
        this.status = "ACTIVE";
    }


    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getContractDate() {
        return contractDate;
    }

    public void setContractDate(LocalDateTime contractDate) {
        this.contractDate = contractDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Car getCar() {
        return car;
    }

    public void setCar(Car car) {
        this.car = car;
    }

    public CustomerJPA getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerJPA customer) {
        this.customer = customer;
    }

    // Méthodes utilitaires
    public int getTotalPrice() {
        return car != null ? car.getPrice() : 0;
    }

    public String getCarInfo() {
        if (car != null && car.getCarModel() != null) {
            return car.getCarModel().getBrand() + " " + car.getCarModel().getModel() + " (" + car.getPlateNumber() + ")";
        }
        return "No car assigned";
    }

   
}
