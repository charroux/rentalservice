package com.charroux.carRental.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.charroux.carRental.service.RentalService;
import com.charroux.carRental.web.CarRentalRestService;

import org.springframework.http.MediaType;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class CarRentalRestServiceTests {

    @Mock
    RentalService rentalService;

    MockMvc mockMvc;

    @BeforeEach
    void setup() {
        CarRentalRestService controller = new CarRentalRestService(rentalService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void testZeroVoiture() throws Exception {
        when(rentalService.carsToBeRented()).thenReturn(java.util.Collections.emptyList());
        mockMvc.perform(get("/cars").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

}