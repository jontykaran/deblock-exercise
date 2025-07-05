package org.deblock.exercise.service;

import org.deblock.exercise.controller.dto.SearchRequest;
import org.deblock.exercise.domain.FlightResponse;
import org.deblock.exercise.domain.FlightSupplierClient;
import org.deblock.exercise.exception.FlightSearchException;
import org.deblock.exercise.exception.FlightSupplierException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SearchFlightsServiceTest {

    private FlightSupplierClient supplier1;
    private FlightSupplierClient supplier2;
    private SearchFlightsService systemUnderTest;

    private SearchRequest request;

    @BeforeEach
    void setup() {
        supplier1 = mock(FlightSupplierClient.class);
        supplier2 = mock(FlightSupplierClient.class);
        systemUnderTest = new SearchFlightsService(List.of(supplier1, supplier2));

        request = new SearchRequest(
                "LHR", "AMS",
                LocalDate.of(2025, 7, 20),
                LocalDate.of(2025, 7, 25),
                1
        );
    }

    @Test
    void shouldReturnCombinedAndSortedResultsFromAllSuppliers() {
        // arrange
        FlightResponse response1 = new FlightResponse("Airline1", "CrazyAir", 200.0, "LHR", "AMS",
                LocalDateTime.of(2025, 7, 20, 10, 0),
                LocalDateTime.of(2025, 7, 20, 12, 0));

        FlightResponse response2 = new FlightResponse("Airline2", "ToughJet", 250.0, "LHR", "AMS",
                LocalDateTime.of(2025, 7, 20, 10, 0),
                LocalDateTime.of(2025, 7, 20, 12, 0));

        FlightResponse response3 = new FlightResponse("Airline3", "ToughJet", 150.0, "LHR", "AMS",
                LocalDateTime.of(2025, 7, 20, 14, 0),
                LocalDateTime.of(2025, 7, 20, 16, 0));

        when(supplier1.search(request)).thenReturn(CompletableFuture.completedFuture(List.of(response1)));
        when(supplier2.search(request)).thenReturn(CompletableFuture.completedFuture(List.of(response2, response3)));

        // act
        List<FlightResponse> results = systemUnderTest.search(request);

        // assert
        assertEquals(3, results.size());
        assertEquals(response3, results.get(0));  // Cheapest first
        assertEquals(response1, results.get(1));
        assertEquals(response2, results.get(2));
    }

    @Test
    void shouldIgnoreSupplierThatThrowsException() {
        // arrange
        FlightResponse response1 = new FlightResponse("Airline1", "CrazyAir", 200.0, "LHR", "AMS",
                LocalDateTime.of(2025, 7, 20, 10, 0),
                LocalDateTime.of(2025, 7, 20, 12, 0));

        when(supplier1.search(request)).thenReturn(CompletableFuture.completedFuture(List.of(response1)));
        when(supplier2.search(request)).thenReturn(CompletableFuture.failedFuture(new FlightSupplierException("Supplier failed with exception")));

        // act
        List<FlightResponse> results = systemUnderTest.search(request);

        // assert
        assertEquals(1, results.size());
        assertEquals(response1, results.get(0));
    }

    @Test
    void shouldReturnEmptyListIfAllSuppliersFail() {
        // arrange
        when(supplier1.search(request)).thenReturn(CompletableFuture.failedFuture(new FlightSupplierException("Supplier fetch failed")));
        when(supplier2.search(request)).thenReturn(CompletableFuture.failedFuture(new FlightSupplierException("Supplier fetch failed")));

        // act and assert
        assertThrows(FlightSearchException.class, () -> systemUnderTest.search(request));
    }
}