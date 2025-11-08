package com.charroux.carRental.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collection;

@Entity
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    
    private String plateNumber;
    private int price;
    
    // Relation ManyToOne vers CarModel (un modèle peut avoir plusieurs voitures)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_model_id")
    private CarModelJPA carModel;

    // Relation OneToOne optionnelle vers BiddingJPA (0 ou 1) - Enchère gagnante
    @OneToOne(mappedBy = "car", fetch = FetchType.LAZY, optional = true)
    @JsonIgnore // Pour éviter la sérialisation cyclique
    private BiddingJPA winningBid;

    // Relation OneToMany vers RentalContract
    // Une voiture peut avoir plusieurs contrats de location (historique)
    @OneToMany(mappedBy = "car", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore // Pour éviter la sérialisation cyclique
    private Collection<RentalContract> rentalContracts = new ArrayList<>();

    // Constructeurs
    public Car() {
    }

    public Car(String plateNumber, int price) {
        this.plateNumber = plateNumber;
        this.price = price;
    }

    // Getters et Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public CarModelJPA getCarModel() {
        return carModel;
    }

    public void setCarModel(CarModelJPA carModel) {
        this.carModel = carModel;
    }

    public BiddingJPA getWinningBid() {
        return winningBid;
    }

    public void setWinningBid(BiddingJPA winningBid) {
        this.winningBid = winningBid;
        if (winningBid != null) {
            winningBid.setCar(this);
        }
    }

    public Collection<RentalContract> getRentalContracts() {
        return rentalContracts;
    }

    public void setRentalContracts(Collection<RentalContract> rentalContracts) {
        this.rentalContracts = rentalContracts;
    }

    // Méthodes utilitaires pour gérer la relation bidirectionnelle
    public void addRentalContract(RentalContract contract) {
        rentalContracts.add(contract);
        contract.setCar(this);
    }

    public void removeRentalContract(RentalContract contract) {
        rentalContracts.remove(contract);
        contract.setCar(null);
    }

    public RentalContract getCurrentActiveContract() {
        return rentalContracts.stream()
                .filter(contract -> "ACTIVE".equals(contract.getStatus()))
                .findFirst()
                .orElse(null);
    }

    public boolean isCurrentlyRented() {
        return getCurrentActiveContract() != null;
    }

    // Méthodes utilitaires
    // Méthodes utilitaires pour la relation bidirectionnelle avec BiddingJPA
    public boolean hasWinningBid() {
        return winningBid != null;
    }

    public void clearWinningBid() {
        if (winningBid != null) {
            winningBid.setCar(null);
            winningBid = null;
        }
    }

    @Override
    public String toString() {
        return "Car{" +
                "id=" + id +
                ", plateNumber='" + plateNumber + '\'' +
                ", price=" + price +
                ", contractsCount=" + rentalContracts.size() +
                ", hasWinningBid=" + hasWinningBid() +
                '}';
    }
}


