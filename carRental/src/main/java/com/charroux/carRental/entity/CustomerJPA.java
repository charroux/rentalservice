package com.charroux.carRental.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.ArrayList;

@Entity
public class CustomerJPA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;

    // Relation OneToMany vers RentalContract
    // Un client peut avoir plusieurs contrats
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<RentalContract> rentalContracts = new ArrayList<>();

    public CustomerJPA() {
    }

    public CustomerJPA(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public Long getId() {
        return id;
    }   

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<RentalContract> getRentalContracts() {
        return rentalContracts;
    }

    public void setRentalContracts(List<RentalContract> rentalContracts) {
        this.rentalContracts = rentalContracts;
    }

    // Méthodes utilitaires pour gérer la relation bidirectionnelle
    public void addRentalContract(RentalContract rentalContract) {
        rentalContracts.add(rentalContract);
        rentalContract.setCustomer(this);
    }

    public void removeRentalContract(RentalContract rentalContract) {
        rentalContracts.remove(rentalContract);
        rentalContract.setCustomer(null);
    }
}