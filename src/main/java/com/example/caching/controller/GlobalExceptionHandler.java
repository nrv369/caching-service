package com.example.caching.controller;

import com.example.caching.exception.CacheInitializationException;
import com.example.caching.exception.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

/**
 * Global exception handler for the REST API.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles EntityNotFoundException, typically for GET/REMOVE operations.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEntityNotFoundException(EntityNotFoundException ex) {
        log.warn("Not Found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND) // 404
                .body(Map.of("error", "Not Found", "message", ex.getMessage()));
    }

    /**
     * Handles IllegalArgumentException, typically for bad input (e.g., null IDs).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Bad Request: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) // 400
                .body(Map.of("error", "Bad Request", "message", ex.getMessage()));
    }

    /**
     * Handles CacheInitializationException, which is a critical setup error.
     */
    @ExceptionHandler(CacheInitializationException.class)
    public ResponseEntity<Map<String, String>> handleCacheInitializationException(CacheInitializationException ex) {
        log.error("Internal Server Error during initialization: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR) // 500
                .body(Map.of("error", "Internal Server Error", "message", "Cache Service failed to initialize: " + ex.getMessage()));
    }

    /**
     * Catch-all for any other unhandled exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAllUncaughtException(Exception ex) {
        log.error("An unexpected error occurred: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR) // 500
                .body(Map.of("error", "Internal Server Error", "message", "An unexpected error occurred."));
    }
}