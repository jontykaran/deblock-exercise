package org.deblock.exercise.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FlightSupplierException.class)
    public ResponseEntity<?> handleFlightSupplierException(FlightSupplierException ex) {
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now(),
                "error", "Flight supplier error",
                "message", ex.getMessage()
        );
        return new ResponseEntity<>(body, HttpStatus.BAD_GATEWAY);
    }

    public ResponseEntity<?> handleFlightSearchException(FlightSearchException ex) {
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now(),
                "error", "Flight search failed due to supplier failure",
                "message", ex.getMessage()
        );
        return new ResponseEntity<>(body, HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<?> handleBindException(BindException ex) {
        String messages = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((msg1, msg2) -> msg1 + "; " + msg2)
                .orElse("Validation failed");

        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now(),
                "error", "Invalid Input",
                "message", messages
        );
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneralException(Exception ex) {
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now(),
                "error", "Internal server error",
                "message", ex.getMessage()
        );
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
