package com.charroux.carRental.entity;

import org.springframework.data.repository.CrudRepository;

public interface CarModelJPARepository extends CrudRepository<CarModelJPA, Long> {

    // Méthode pour trouver un modèle par marque et modèle
    CarModelJPA findByBrandAndModel(String brand, String model);
    
}
