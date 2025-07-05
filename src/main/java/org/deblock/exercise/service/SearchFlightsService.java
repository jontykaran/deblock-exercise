package org.deblock.exercise.service;

import org.deblock.exercise.controller.dto.SearchRequest;
import org.deblock.exercise.domain.FlightResponse;
import org.deblock.exercise.domain.FlightSupplierClient;
import org.deblock.exercise.exception.FlightSearchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class SearchFlightsService {
    private final List<FlightSupplierClient> suppliers;
    private static final Logger logger = LoggerFactory.getLogger(SearchFlightsService.class);

    public SearchFlightsService(List<FlightSupplierClient> suppliers) {
        this.suppliers = suppliers;
    }

    public List<FlightResponse> search(SearchRequest request) {
        List<CompletableFuture<List<FlightResponse>>> futures = suppliers.stream()
                .map(supplier -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return supplier.search(request).join();
                    } catch (Exception e) {
                        throw new RuntimeException("Failed supplier: " + supplier.getClass().getSimpleName(), e);
                    }
                }))
                .toList();

        List<FlightResponse> allResults = new ArrayList<>();
        List<Throwable> failures = new ArrayList<>();

        for (CompletableFuture<List<FlightResponse>> future : futures) {
            try {
                allResults.addAll(future.join());
            } catch (Exception e) {
                failures.add(e);
            }
        }

        if (failures.size() == suppliers.size()) {
            throw new FlightSearchException("Failed to fetch flight search details from all suppliers", failures.get(0));
        }

        return allResults.stream()
                .sorted(Comparator.comparingDouble(FlightResponse::fare))
                .toList();

    }
}
