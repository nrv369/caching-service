package com.example.caching.exception;

/**
 * Custom exception thrown when an entity is not found in the cache or database.
 */
public class EntityNotFoundException extends Exception {

    public EntityNotFoundException(String message) {
        super(message);
    }
}