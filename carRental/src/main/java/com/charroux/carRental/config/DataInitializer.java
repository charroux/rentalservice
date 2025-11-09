package com.charroux.carRental.config;

import com.charroux.carRental.entity.CarModelJPA;
import com.charroux.carRental.entity.CarModelJPARepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer {

    @Autowired
    private CarModelJPARepository carModelJPARepository;

    @EventListener(ApplicationReadyEvent.class)
    public void initData() {
        System.out.println("=== Initialisation des données de test ===");
        
        // Vérifier si des données existent déjà
        if (carModelJPARepository.count() > 0) {
            System.out.println("Des données existent déjà, pas d'initialisation nécessaire");
            return;
        }

        // Créer des modèles de voiture de test
        CarModelJPA tesla = new CarModelJPA();
        tesla.setBrand("Tesla");
        tesla.setModel("Model 3");
        tesla.setLowestPrice(45);
        tesla.setHighestPrice(65);

        CarModelJPA bmw = new CarModelJPA();
        bmw.setBrand("BMW");
        bmw.setModel("X5");
        bmw.setLowestPrice(80);
        bmw.setHighestPrice(120);

        CarModelJPA audi = new CarModelJPA();
        audi.setBrand("Audi");
        audi.setModel("A4");
        audi.setLowestPrice(60);
        audi.setHighestPrice(90);

        CarModelJPA mercedes = new CarModelJPA();
        mercedes.setBrand("Mercedes");
        mercedes.setModel("C-Class");
        mercedes.setLowestPrice(70);
        mercedes.setHighestPrice(100);

        carModelJPARepository.save(tesla);
        carModelJPARepository.save(bmw);
        carModelJPARepository.save(audi);
        carModelJPARepository.save(mercedes);

        System.out.println("=== 4 modèles de voiture créés ===");
        System.out.println("Tesla Model 3: " + tesla.getLowestPrice() + "-" + tesla.getHighestPrice() + "€/jour");
        System.out.println("BMW X5: " + bmw.getLowestPrice() + "-" + bmw.getHighestPrice() + "€/jour");
        System.out.println("Audi A4: " + audi.getLowestPrice() + "-" + audi.getHighestPrice() + "€/jour");
        System.out.println("Mercedes C-Class: " + mercedes.getLowestPrice() + "-" + mercedes.getHighestPrice() + "€/jour");
    }
}