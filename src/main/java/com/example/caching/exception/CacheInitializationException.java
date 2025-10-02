package com.example.caching.exception;

/**
 * Custom exception thrown during service initialization failure (e.g., bad configuration).
 */
public class CacheInitializationException extends RuntimeException {

    public CacheInitializationException(String message) {
        super(message);
    }
}