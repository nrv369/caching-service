package com.example.caching.controller;

import com.example.caching.model.Entity;
import com.example.caching.service.CacheService;
import com.example.caching.exception.EntityNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for the Cache Service API.
 */
@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/cache")
@Tag(name = "Caching Service API", description = "LRU Cache management and entity CRUD operations with persistence.")
public class CacheController {

    private static final Logger log = LoggerFactory.getLogger(CacheController.class);
    private final CacheService cacheService;

    @PostMapping
    @Operation(summary = "Add/Update an entity", description = "Adds the entity to the cache. If cache is full, evicts the LRU element to the database.")
    public ResponseEntity<String> addEntity(@RequestBody Entity entity) {
        log.info("API: Received request to add entity with ID: {}", entity.getId());
        cacheService.add(entity);
        return new ResponseEntity<>("Entity added/updated successfully.", HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve an entity", description = "Tries to get the entity from cache (LRU touch), or from the database if a miss occurs.")
    public ResponseEntity<Entity> getEntity(
            @Parameter(description = "Unique ID of the entity to retrieve", required = true)
            @PathVariable String id) throws EntityNotFoundException {
        log.info("API: Received request to get entity with ID: {}", id);
        Entity entity = cacheService.get(id);
        return new ResponseEntity<>(entity, HttpStatus.OK);
    }

    @DeleteMapping("/item/{id}")
    @Operation(summary = "Remove an entity", description = "Removes the entity from both the internal cache and the database.")
    public ResponseEntity<String> removeEntity(
            @Parameter(description = "Unique ID of the entity to remove", required = true)
            @PathVariable String id) throws EntityNotFoundException {
        log.info("API: Received request to remove entity with ID: {}", id);
        cacheService.remove(id);
        return new ResponseEntity<>("Entity removed from cache and database successfully.", HttpStatus.OK);
    }

    @DeleteMapping("/all")
    @Operation(summary = "Remove all entities", description = "Removes all entities from both the internal cache and the database.")
    public ResponseEntity<String> removeAll() {
        log.warn("API: Received request to REMOVE ALL entities from cache AND database.");
        cacheService.removeAll();
        return new ResponseEntity<>("All entities cleared from cache and database.", HttpStatus.NO_CONTENT);
    }

    @DeleteMapping("/clear-cache")
    @Operation(summary = "Clear internal cache", description = "Clears only the internal cache. Database entries are unaffected.")
    public ResponseEntity<String> clearCache() {
        log.warn("API: Received request to CLEAR the internal cache only.");
        cacheService.clear();
        return new ResponseEntity<>("Internal cache cleared successfully.", HttpStatus.NO_CONTENT);
    }

    @GetMapping("/status")
    @Operation(summary = "Get cache status", description = "Returns the maximum size of the cache.")
    public ResponseEntity<String> getStatus() {
        return new ResponseEntity<>("Cache Max Size: " + cacheService.getMaxSize(), HttpStatus.OK);
    }
}