package org.deblock.exercise.adapter.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ToughJetResponse(String carrier, Double basePrice, Double tax, Double discount, String departureAirportName, String arrivalAirportName, LocalDateTime outboundDateTime, LocalDateTime inboundDateTime) {
}