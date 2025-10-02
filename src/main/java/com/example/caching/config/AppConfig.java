package com.example.caching.config;

import com.example.caching.repository.DatabaseRepository;
import com.example.caching.service.CacheService;
import com.example.caching.service.LRUCacheServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for defining application-wide beans and settings.
 */
@Configuration
public class AppConfig {

    /**
     * Defines the CacheService bean explicitly, injecting the configured max size
     * and the DatabaseRepository dependency.
     *
     * Note: If you use this configuration, you should remove the @Service
     * annotation from LRUCacheServiceImpl.
     *
     * @param maxSize The configured maximum size from application.properties (or default 5).
     * @param databaseRepository The repository dependency.
     * @return The initialized CacheService instance.
     */
    @Bean
    public CacheService cacheService(
            @Value("${cache.max-size:5}") int maxSize, 
            DatabaseRepository databaseRepository) {
        
        // This is where you create and return the concrete implementation
        return new LRUCacheServiceImpl(maxSize, databaseRepository);
    }
}