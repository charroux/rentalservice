package com.charroux.carRental.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CarRepositoryTests {

    @Mock
    private CarRepository carRepository;

    @Test
    public void cerRepository() {
        Car car = new Car("AA11BB", "Ferrari", "F40", 1000);
        when(carRepository.save(car)).thenReturn(car);
        when(carRepository.findAll()).thenReturn(Collections.singletonList(car));
    }

    @Test
    void aSingleCar(){
        Car car = new Car("AA11BB", "Ferrari", "F40", 1000);
        when(carRepository.save(car)).thenReturn(car);
        when(carRepository.findById(car.getId())).thenReturn(Optional.of(car));
    }

    @Test
    void supprimerVoiture(){
        Car car = new Car("AA11BB", "Ferrari", "F40", 1000);
        when(carRepository.save(car)).thenReturn(car);
        carRepository.delete(car);
        verify(carRepository, times(1)).delete(car);
    }

}