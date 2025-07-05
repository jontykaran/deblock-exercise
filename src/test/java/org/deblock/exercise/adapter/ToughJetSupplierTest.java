package org.deblock.exercise.adapter;

import org.deblock.exercise.adapter.dto.ToughJetResponse;
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

class ToughJetSupplierTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ToughJetSupplier toughJetSupplier;

    private SearchRequest searchRequest;

    private String url = "http://mock-toughjet.com/flights?from=LHR&to=AMS&outboundDate=2025-07-20&inboundDate=2025-07-25&numberOfAdults=2";

    // arrange
    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(toughJetSupplier, "apiUrl", "http://mock-toughjet.com/flights");

        searchRequest = new SearchRequest("LHR", "AMS", LocalDate.parse("2025-07-20"), LocalDate.parse("2025-07-25"), 2);
    }

    @Test
    void testSearch_singleFlightReturned() {
        // arrange
        ToughJetResponse toughJetResponse = new ToughJetResponse(
            "ToughJet",
            100.0,
            10.0,
            10.0,
            "LHR",
            "AMS",
                LocalDateTime.of(2025, 7, 10, 10, 0),
                LocalDateTime.of(2025, 7, 20, 20, 0)
        );

        List<ToughJetResponse> clientResponses = List.of(toughJetResponse);

        ResponseEntity<List<ToughJetResponse>> responseEntity =
                new ResponseEntity<>(clientResponses, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(url),
                any(),
                isNull(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        FlightResponse expectedFlightResponse = new FlightResponse(
                "ToughJet",
                "ToughJet",
                99.0,
                "LHR",
                "AMS",
                LocalDateTime.of(2025, 7, 10, 10, 0),
                LocalDateTime.of(2025, 7, 20, 20, 0)
        );
        // act
        CompletableFuture<List<FlightResponse>> future = toughJetSupplier.search(searchRequest);
        List<FlightResponse> results = future.join();

        // assert
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(expectedFlightResponse, results.get(0));
    }

    @Test
    void testSearch_multipleFlightsReturned() {
        // arrange
        ToughJetResponse response1 = new ToughJetResponse(
                "Carrier1",
                50.0,
                5.0,
                10.0,
                "LHR",
                "AMS",
                LocalDateTime.of(2025, 7, 20, 8, 0),
                LocalDateTime.of(2025, 7, 25, 18, 0)
        );

        ToughJetResponse response2 = new ToughJetResponse(
                "Carrier2",
                150.0,
                15.0,
                50.0,
                "LHR",
                "AMS",
                LocalDateTime.of(2025, 7, 20, 9, 0),
                LocalDateTime.of(2025, 7, 25, 19, 0)
        );

        List<ToughJetResponse> clientResponses = List.of(response1, response2);

        ResponseEntity<List<ToughJetResponse>> responseEntity =
                new ResponseEntity<>(clientResponses, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(url),
                any(),
                isNull(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        List<FlightResponse>  expectedFlightResponses = List.of(
                new FlightResponse(
                        "Carrier1",
                        "ToughJet",
                        49.5,
                        "LHR",
                        "AMS",
                        LocalDateTime.of(2025, 7, 20, 8, 0),
                        LocalDateTime.of(2025, 7, 25, 18, 0)
                ),
                new FlightResponse(
                        "Carrier2",
                        "ToughJet",
                        82.5,
                        "LHR",
                        "AMS",
                        LocalDateTime.of(2025, 7, 20, 9, 0),
                        LocalDateTime.of(2025, 7, 25, 19, 0)
                )
        );

        // act
        CompletableFuture<List<FlightResponse>> future = toughJetSupplier.search(searchRequest);
        List<FlightResponse> results = future.join();

        // assert
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals(expectedFlightResponses, results);
    }

    @Test
    void testSearch_noFlightsReturned() {
        // arrange
        ResponseEntity<List<ToughJetResponse>> responseEntity =
                new ResponseEntity<>(List.of(), HttpStatus.OK);

        when(restTemplate.exchange(
                eq(url),
                any(),
                isNull(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        // act
        List<FlightResponse> results = toughJetSupplier.search(searchRequest).join();
        // assert
        assertTrue(results.isEmpty());
    }

    @Test
    void testSearch_ThrowsExceptionForBadHttpStatus() {
        // arrange
        ResponseEntity<List<ToughJetResponse>> responseEntity =
                new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);

        when(restTemplate.exchange(
                eq(url),
                any(),
                isNull(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        // act and assert
        FlightSupplierException ex = assertThrows(FlightSupplierException.class, () ->
                toughJetSupplier.search(searchRequest).join());
        assertTrue(ex.getMessage().contains("Failed to fetch flights"));
    }

    @Test
    void testSearch_ThrowsExceptionForRestClientException() {
        // arrange
        when(restTemplate.exchange(
                eq(url),
                any(),
                isNull(),
                any(ParameterizedTypeReference.class)))
                .thenThrow(new RestClientException("Connection error"));

        // act and assert
        FlightSupplierException ex = assertThrows(FlightSupplierException.class, () ->
                toughJetSupplier.search(searchRequest).join());
        assertTrue(ex.getMessage().contains("Error calling supplier"));
    }
}