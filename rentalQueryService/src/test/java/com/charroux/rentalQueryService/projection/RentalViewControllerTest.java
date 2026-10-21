package com.charroux.rentalQueryService.projection;

import com.charroux.rentalQueryService.web.RentalViewController;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RentalViewControllerTest {
    @Test
    void returnsNotFoundWhenProjectionHasNotArrived() {
        RentalViewRepository repository = mock(RentalViewRepository.class);
        when(repository.findByRentalId("RENT-404")).thenReturn(Optional.empty());

        assertEquals(404, new RentalViewController(repository).find("RENT-404").getStatusCode().value());
    }

    @Test
    void exposesInsuranceFirstProjectionAsPartial() {
        RentalViewRepository repository = mock(RentalViewRepository.class);
        RentalView view = new RentalView("RENT-42");
        view.applyInsurance("accepted-1", Instant.parse("2026-10-20T09:20:00Z"), "auction-1",
            42L, "EDU-042", "Tesla", "Model 3", 62, 65, 3, 7, "ACCEPTED");
        when(repository.findByRentalId("RENT-42")).thenReturn(Optional.of(view));

        var response = new RentalViewController(repository).find("RENT-42");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("PARTIAL", response.getBody().projectionState());
        assertNull(response.getBody().rental());
        assertEquals("ACCEPTED", response.getBody().insurance().getStatus());
    }
}
