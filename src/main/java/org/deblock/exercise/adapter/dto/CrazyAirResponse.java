package org.deblock.exercise.adapter.dto;

import java.time.LocalDateTime;

public record CrazyAirResponse(String airline, Double price, String cabinClass, String departureAirportCode, String destinationAirportCode, LocalDateTime departureDate, LocalDateTime arrivalDate) {
}