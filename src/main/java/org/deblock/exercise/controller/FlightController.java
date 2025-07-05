package org.deblock.exercise.controller;

import org.deblock.exercise.controller.dto.SearchRequest;
import org.deblock.exercise.domain.FlightResponse;
import org.deblock.exercise.service.SearchFlightsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/flights")
@Validated
public class FlightController {

    @Autowired
    private final SearchFlightsService searchFlightsService;

    public FlightController(SearchFlightsService searchFlightsService) {
        this.searchFlightsService = searchFlightsService;
    }

    @GetMapping
    public List<FlightResponse> searchFlights(@Valid @ModelAttribute SearchRequest request) {
        return searchFlightsService.search(request);
    }
}
