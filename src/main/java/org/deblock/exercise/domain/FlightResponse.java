package org.deblock.exercise.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FlightResponse(String airline, String supplier, Double fare, String departureAirportCode, String destinationAirportCode, LocalDateTime departureDate, LocalDateTime arrivalDate) {
}
