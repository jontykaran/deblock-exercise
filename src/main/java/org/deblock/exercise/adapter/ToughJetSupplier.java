package org.deblock.exercise.adapter;

import org.deblock.exercise.adapter.dto.ToughJetResponse;
import org.deblock.exercise.controller.dto.SearchRequest;
import org.deblock.exercise.domain.FlightResponse;
import org.deblock.exercise.domain.FlightSupplierClient;
import org.deblock.exercise.exception.FlightSupplierException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class ToughJetSupplier implements FlightSupplierClient {

    private final RestTemplate restTemplate;

    @Value("${toughjet.api.url}")
    String apiUrl;

    private final String supplierName = "ToughJet";

    private static final Logger logger = LoggerFactory.getLogger(ToughJetSupplier.class);

    public ToughJetSupplier(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    @Async
    public CompletableFuture<List<FlightResponse>> search(SearchRequest request) {
        String url = UriComponentsBuilder
                .fromHttpUrl(apiUrl)
                .queryParam("from", request.origin())
                .queryParam("to", request.destination())
                .queryParam("outboundDate", request.departureDate())
                .queryParam("inboundDate", request.returnDate())
                .queryParam("numberOfAdults", request.numberOfPassengers())
                .toUriString();

        logger.info("Calling {} API with URL: {}", supplierName, url);

        try {
            ResponseEntity<List<ToughJetResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ToughJetResponse>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                 List<FlightResponse> result = response.getBody().stream()
                        .map(res -> {
                            double fare = (res.basePrice() + res.tax()) * (1 - res.discount() / 100);
                            double roundedFare = Math.round(fare * 100.0) / 100.0;

                            return new FlightResponse(
                                    res.carrier(),
                                    supplierName,
                                    roundedFare,
                                    res.departureAirportName(),
                                    res.arrivalAirportName(),
                                    res.outboundDateTime().atOffset(ZoneOffset.UTC).toLocalDateTime(),
                                    res.inboundDateTime().atOffset(ZoneOffset.UTC).toLocalDateTime()
                            );
                        })
                        .toList();
                 return CompletableFuture.completedFuture(result);
            } else {
                logger.error("Failed response from {} API: HTTP {}", supplierName, response.getStatusCode());
                throw new FlightSupplierException(String.format("Failed to fetch flights from supplier %s: HTTP %s",
                        supplierName, response.getStatusCode()));
            }
        } catch (RestClientException e) {
            logger.error("Exception when calling {} API", supplierName, e);
            throw new FlightSupplierException(String.format("Error calling supplier %s API", supplierName), e);
        }
    }
}