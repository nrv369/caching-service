package com.example.caching.repository;

import com.example.caching.model.Entity;
import com.example.caching.exception.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock Repository to simulate database interaction.
 * Uses ConcurrentHashMap as an in-memory database for this example.
 */
@Repository
public class DatabaseRepository {

    private static final Logger log = LoggerFactory.getLogger(DatabaseRepository.class);
    
    // Stores entityId -> Entity object
    public final ConcurrentHashMap<String, Entity> mockDatabase = new ConcurrentHashMap<>();

    /**
     * Saves an entity to the mock database (used for eviction and initial save).
     * @param entity The entity to save.
     * @return The saved entity.
     */
    public Entity save(Entity entity) {
        if (entity == null || entity.getId() == null) {
            log.error("Attempted to save a null entity or an entity with a null ID.");
            throw new IllegalArgumentException("Entity or entity ID cannot be null for saving.");
        }
        mockDatabase.put(entity.getId(), entity);
        log.info("DB: Entity with ID '{}' saved/updated successfully.", entity.getId());
        return entity;
    }

    /**
     * Retrieves an entity by its ID from the mock database.
     * @param id The ID of the entity to retrieve.
     * @return An Optional containing the entity, or empty if not found.
     */
    public Optional<Entity> findById(String id) {
        log.debug("DB: Attempting to retrieve entity with ID '{}'.", id);
        return Optional.ofNullable(mockDatabase.get(id));
    }

    /**
     * Deletes an entity by its ID from the mock database.
     * @param id The ID of the entity to delete.
     * @throws EntityNotFoundException if the entity does not exist.
     */
    public void deleteById(String id) throws EntityNotFoundException {
        if (mockDatabase.remove(id) == null) {
            log.warn("DB: Attempted to delete non-existent entity with ID '{}'.", id);
            throw new EntityNotFoundException("Cannot delete: Entity with ID " + id + " not found in DB.");
        }
        log.info("DB: Entity with ID '{}' deleted successfully.", id);
    }

    /**
     * Deletes all entities from the mock database.
     */
    public void deleteAll() {
        int count = mockDatabase.size();
        mockDatabase.clear();
        log.warn("DB: All {} entities removed from the database.", count);
    }
}