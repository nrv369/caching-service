package com.example.caching.service;

import com.example.caching.model.Entity;
import com.example.caching.exception.EntityNotFoundException;

/**
 * Defines the contract for the Caching Service.
 */
public interface CacheService {

    /**
     * Adds an entity to the internal cache. If the cache is full,
     * the least-used element is evicted to the database.
     * @param entity The entity to add.
     */
    void add(Entity entity);

    /**
     * Removes an entity from the internal cache and the database.
     * @param id The ID of the entity to remove.
     * @throws EntityNotFoundException if the entity is not found in the DB.
     */
    void remove(String id) throws EntityNotFoundException;

    /**
     * Removes all entities from the internal cache and the database.
     */
    void removeAll();

    /**
     * Retrieves an entity, checking the internal cache first, then the database.
     * If found in the database, it's promoted to the cache.
     * @param id The ID of the entity to retrieve.
     * @return The retrieved entity.
     * @throws EntityNotFoundException if the entity is not found in cache or DB.
     */
    Entity get(String id) throws EntityNotFoundException;

    /**
     * Clears the internal cache. No impact on entries in the database.
     */
    void clear();

    /**
     * Retrieves the configured maximum size of the cache.
     * @return The max size.
     */
    int getMaxSize();
}