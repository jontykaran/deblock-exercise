package org.deblock.exercise.adapter;

import org.deblock.exercise.adapter.dto.CrazyAirResponse;
import org.deblock.exercise.controller.dto.SearchRequest;
import org.deblock.exercise.domain.FlightResponse;
import org.deblock.exercise.exception.FlightSupplierException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class CrazyAirSupplierTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private CrazyAirSupplier crazyAirSupplier;

    private String url = "http://mock-crazyair.com/flights?origin=LHR&destination=AMS&departureDate=2025-07-01&returnDate=2025-07-10&passengerCount=2";
    private SearchRequest searchRequest;

    // arrange
    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(crazyAirSupplier, "apiUrl", "http://mock-crazyair.com/flights");
        searchRequest = new SearchRequest("LHR", "AMS",
                LocalDate.parse("2025-07-01"), LocalDate.parse("2025-07-10"), 2);
    }

    @Test
    void testSearch_singleFlightReturned() {
        // arrange
        CrazyAirResponse singleResponse = new CrazyAirResponse(
                "CrazyAir1", 100.0, "E", "LHR", "AMS",
                LocalDateTime.of(2025, 7, 1, 10, 0),
                LocalDateTime.of(2025, 7, 1, 12, 0)
        );
        ResponseEntity<List<CrazyAirResponse>> responseEntity =
                ResponseEntity.ok(List.of(singleResponse));
        when(restTemplate.exchange(
                eq(url),
                any(),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        FlightResponse expectedFlightResponse = new FlightResponse(
                "CrazyAir1",
                "CrazyAir",
                100.0,
                "LHR",
                "AMS",
                LocalDateTime.of(2025, 7, 1, 10, 0),
                LocalDateTime.of(2025, 7, 1, 12, 0)
        );
        // act
        CompletableFuture<List<FlightResponse>> future = crazyAirSupplier.search(searchRequest);
        List<FlightResponse> results = future.join();

        // assert
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(expectedFlightResponse, results.get(0));
    }

    @Test
    void testSearch_multipleFlightsReturned() {
        // arrange
        CrazyAirResponse response1 = new CrazyAirResponse(
                "CrazyAir1", 100.0, "E", "LHR", "AMS",
                LocalDateTime.of(2025, 7, 1, 10, 0),
                LocalDateTime.of(2025, 7, 1, 12, 0)
        );
        CrazyAirResponse response2 = new CrazyAirResponse(
                "CrazyAir2", 150.0, "B", "LHR", "AMS",
                LocalDateTime.of(2025, 7, 1, 14, 0),
                LocalDateTime.of(2025, 7, 1, 16, 0)
        );

        ResponseEntity<List<CrazyAirResponse>> responseEntity =
                ResponseEntity.ok(List.of(response1, response2));

        when(restTemplate.exchange(
                eq(url),
                any(),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        List<FlightResponse> expectedFlightResponses = List.of(
                new FlightResponse(
                    "CrazyAir1", "CrazyAir", 100.0, "LHR", "AMS",
                    LocalDateTime.of(2025, 7, 1, 10, 0),
                    LocalDateTime.of(2025, 7, 1, 12, 0)
                ),
                new FlightResponse(
                    "CrazyAir2", "CrazyAir", 150.0, "LHR", "AMS",
                    LocalDateTime.of(2025, 7, 1, 14, 0),
                    LocalDateTime.of(2025, 7, 1, 16, 0)
                )
        );

        // act
        CompletableFuture<List<FlightResponse>> future = crazyAirSupplier.search(searchRequest);
        List<FlightResponse> results = future.join();

        // assert
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals(expectedFlightResponses, results);
    }

    @Test
    void testSearch_noFlightsReturned() throws Exception {
        // arrange
        ResponseEntity<List<CrazyAirResponse>> responseEntity =
                ResponseEntity.ok(List.of());

        when(restTemplate.exchange(
                eq(url),
                any(),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        // act
        CompletableFuture<List<FlightResponse>> future = crazyAirSupplier.search(searchRequest);
        List<FlightResponse> results = future.get();

        // assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testSearch_ThrowsExceptionForBadHttpStatus() {
        // arrange
        ResponseEntity<List<CrazyAirResponse>> responseEntity =
                new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);

        when(restTemplate.exchange(
                eq(url),
                any(),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        // act and assert
        FlightSupplierException ex = assertThrows(FlightSupplierException.class,
                () -> crazyAirSupplier.search(searchRequest).join());
        assertTrue(ex.getMessage().contains("Failed to fetch flights from supplier CrazyAir"));
    }

    @Test
    void testSearch_ThrowsExceptionForRestClientException() {
        // arrange
        when(restTemplate.exchange(
                eq(url),
                any(),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection refused"));
        // act and assert
        FlightSupplierException ex = assertThrows(FlightSupplierException.class,
                () -> crazyAirSupplier.search(searchRequest).join());
        assertTrue(ex.getMessage().contains("Error calling supplier CrazyAir API"));
    }
}
