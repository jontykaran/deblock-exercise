package org.deblock.exercise.domain;

import org.deblock.exercise.controller.dto.SearchRequest;
import org.springframework.scheduling.annotation.Async;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface FlightSupplierClient {

    @Async
    CompletableFuture<List<FlightResponse>> search(SearchRequest request);
}
