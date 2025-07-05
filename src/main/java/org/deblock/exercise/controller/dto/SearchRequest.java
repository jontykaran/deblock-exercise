package org.deblock.exercise.controller.dto;

import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.*;
import java.time.LocalDate;

public record SearchRequest(
        @NotBlank @Size(min = 3, max = 3) String origin,
        @NotBlank @Size(min = 3, max = 3) String destination,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @NotNull LocalDate departureDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @NotNull LocalDate returnDate,
        @NotNull @Min(1) @Max(4) int numberOfPassengers
) {}