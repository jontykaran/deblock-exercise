package org.deblock.exercise.exception;

public class FlightSupplierException extends RuntimeException {
    public FlightSupplierException(String message) {
        super(message);
    }
    public FlightSupplierException(String message, Throwable cause) {
        super(message, cause);
    }
}
