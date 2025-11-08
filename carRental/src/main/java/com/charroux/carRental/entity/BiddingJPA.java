package com.charroux.carRental.entity;

import jakarta.persistence.*;

@Entity
public class BiddingJPA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;

    double amount;
    
    // Relation OneToOne optionnelle vers Car (0 ou 1)
    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "car_id")
    private Car car;
    
    // Relation ManyToOne vers CarModelJPA (plusieurs biddings pour un modèle)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_model_jpa_id")
    private CarModelJPA carModelJPA;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public Car getCar() {
        return car;
    }

    public void setCar(Car car) {
        this.car = car;
    }

    public CarModelJPA getCarModelJPA() {
        return carModelJPA;
    }

    public void setCarModelJPA(CarModelJPA carModelJPA) {
        this.carModelJPA = carModelJPA;
    }

    @Override
    public String toString() {
        return "BiddingJPA{" +
                "id=" + id +
                ", amount=" + amount +
                ", car=" + (car != null ? car.getPlateNumber() : "none") +
                ", carModel=" + (carModelJPA != null ? carModelJPA.getBrand() + " " + carModelJPA.getModel() : "none") +
                '}';
    }
}
