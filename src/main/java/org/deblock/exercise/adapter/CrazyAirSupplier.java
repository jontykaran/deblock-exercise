package org.deblock.exercise.adapter;

import org.deblock.exercise.adapter.dto.CrazyAirResponse;
import org.deblock.exercise.controller.dto.SearchRequest;
import org.deblock.exercise.domain.FlightResponse;
import org.deblock.exercise.domain.FlightSupplierClient;
import org.deblock.exercise.exception.FlightSupplierException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
public class CrazyAirSupplier implements FlightSupplierClient {

    @Autowired
    private RestTemplate restTemplate;

    private final String supplierName = "CrazyAir";

    @Value("${crazyair.api.url}")
    String apiUrl;

    private static final Logger logger = LoggerFactory.getLogger(CrazyAirSupplier.class);


    @Override
    @Async
    public CompletableFuture<List<FlightResponse>> search(SearchRequest request) {
        String url = UriComponentsBuilder
                .fromHttpUrl(apiUrl)
                .queryParam("origin", request.origin())
                .queryParam("destination", request.destination())
                .queryParam("departureDate", request.departureDate())
                .queryParam("returnDate", request.returnDate())
                .queryParam("passengerCount", request.numberOfPassengers())
                .toUriString();

        logger.info("Calling {} API with URL: {}", supplierName, url);
        try {
            ResponseEntity<List<CrazyAirResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<CrazyAirResponse>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<FlightResponse> result =  response.getBody().stream()
                        .map(res -> new FlightResponse(
                                res.airline(),
                                supplierName,
                                res.price(),
                                res.departureAirportCode(),
                                res.destinationAirportCode(),
                                res.departureDate(),
                                res.arrivalDate()
                        ))
                        .collect(Collectors.toList());
                return CompletableFuture.completedFuture(result);
            } else {
                logger.error("Failed response from {} API: HTTP {}", supplierName, response.getStatusCode());
                throw new FlightSupplierException(String.format("Failed to fetch flights from supplier %s: HTTP %s", supplierName, response.getStatusCode()));
            }
        } catch (RestClientException e) {
            logger.error("Exception when calling {} API", supplierName, e);
            throw new FlightSupplierException(String.format("Error calling supplier %s API", supplierName), e);
        }
    }
}