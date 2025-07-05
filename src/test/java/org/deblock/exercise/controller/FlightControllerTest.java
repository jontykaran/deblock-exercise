package org.deblock.exercise.controller;

import org.deblock.exercise.controller.dto.SearchRequest;
import org.deblock.exercise.domain.FlightResponse;
import org.deblock.exercise.exception.FlightSearchException;
import org.deblock.exercise.service.SearchFlightsService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FlightController.class)
class FlightControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchFlightsService searchFlightsService;


    @Test
    void shouldReturnFlightResults() throws Exception {
        SearchRequest request = new SearchRequest("LHR", "AMS", LocalDate.now(), LocalDate.now().plusDays(3), 1);

        List<FlightResponse> mockResults = List.of(
                new FlightResponse(
                        "CrazyAir", "CrazyAir", 123.45,
                        "LHR", "AMS",
                        LocalDateTime.now(), LocalDateTime.now().plusHours(2))
        );

        when(searchFlightsService.search(Mockito.any())).thenReturn(mockResults);

        mockMvc.perform(get("/flights")
                        .param("origin", request.origin())
                        .param("destination", request.destination())
                        .param("departureDate", request.departureDate().toString())
                        .param("returnDate", request.returnDate().toString())
                        .param("numberOfPassengers", String.valueOf(request.numberOfPassengers()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].airline").value("CrazyAir"))
                .andExpect(jsonPath("$[0].fare").value(123.45));
    }

    @Test
    void shouldHandleServiceException() throws Exception {
        SearchRequest request = new SearchRequest("LHR", "AMS", LocalDate.now(), LocalDate.now().plusDays(3), 1);

        when(searchFlightsService.search(Mockito.any()))
                .thenThrow(new FlightSearchException("Service supplier unavailable"));

        mockMvc.perform(get("/flights")
                        .param("origin", request.origin())
                        .param("destination", request.destination())
                        .param("departureDate", request.departureDate().toString())
                        .param("returnDate", request.returnDate().toString())
                        .param("numberOfPassengers", String.valueOf(request.numberOfPassengers()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void shouldReturnBadRequestWhenInvalidRequest() throws Exception {
        // invalid passengers
        mockMvc.perform(get("/flights")
                        .param("origin", "LHR")
                        .param("destination", "AMS")
                        .param("departureDate", "2025-07-05")
                        .param("returnDate", "2025-07-08")
                        .param("numberOfPassengers", "5") // too many
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // invalid origin
        mockMvc.perform(get("/flights")
                        .param("origin", "LH")
                        .param("destination", "AMS")
                        .param("departureDate", "2025-07-05")
                        .param("returnDate", "2025-07-08")
                        .param("numberOfPassengers", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // invalid destination
        mockMvc.perform(get("/flights")
                        .param("origin", "LHR")
                        .param("destination", "AMS4") //invalid destination
                        .param("departureDate", "2025-07-05")
                        .param("returnDate", "2025-07-08")
                        .param("numberOfPassengers", "2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // invalid date
        mockMvc.perform(get("/flights")
                        .param("origin", "LHR")
                        .param("destination", "AMS")
                        .param("departureDate", "2025-07-55") //invalid date
                        .param("returnDate", "2025-07-08")
                        .param("numberOfPassengers", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // missing input
        mockMvc.perform(get("/flights")
                        .param("origin", "LHR")
                        .param("destination", "AMS")
                        .param("returnDate", "2025-07-08")
                        .param("numberOfPassengers", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}