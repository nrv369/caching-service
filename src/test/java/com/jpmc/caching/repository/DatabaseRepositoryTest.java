package com.jpmc.caching.repository;

import com.example.caching.model.Entity;
import com.example.caching.repository.DatabaseRepository;
import com.example.caching.exception.EntityNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the DatabaseRepository class, which simulates
 * database interaction using a ConcurrentHashMap.
 * * Note: Accessing mockDatabase.size() is done here for simple state verification,
 * but should ideally be done through another public method if this were a production repository.
 */
class DatabaseRepositoryTest {

    private DatabaseRepository repository;

    // A helper method to create a valid entity for testing (assuming Entity has String id and String data)
    private Entity createTestEntity(String id) {
        // Assuming Entity has a constructor that takes ID and data
        return new Entity(id, "Test Data for " + id); 
    }

    // Setup a clean repository instance before each test
    @BeforeEach
    void setUp() {
        repository = new DatabaseRepository();
    }

    // --- Tests for save(Entity entity) ---

    @Test
    void save_NewEntity_ShouldBeSuccessful() {
        Entity newEntity = createTestEntity("E1");

        Entity savedEntity = repository.save(newEntity);

        // Verify the saved entity is returned
        assertNotNull(savedEntity);
        assertEquals("E1", savedEntity.getId());
        
        // Verify it was actually saved and can be retrieved
        assertTrue(repository.findById("E1").isPresent());
    }

    @Test
    void save_ExistingEntity_ShouldBeUpdated() {
        final String entityId = "E_UPDATE";
        
        // 1. Save initial entity
        repository.save(createTestEntity(entityId));
        
        // 2. Create entity with the same ID but new data
        Entity updatedEntity = new Entity(entityId, "NEW DATA VALUE");
        repository.save(updatedEntity);

        // 3. Verify the new data is retrieved, confirming the update
        Optional<Entity> found = repository.findById(entityId);
        assertTrue(found.isPresent());
        assertEquals("NEW DATA VALUE", found.get().getData(), "Data should reflect the update.");
    }

    @Test
    void save_NullEntity_ShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            repository.save(null);
        }, "Saving a null entity should throw IllegalArgumentException.");
    }

    @Test
    void save_EntityWithNullId_ShouldThrowIllegalArgumentException() {
        Entity invalidEntity = new Entity(null, "Data without ID");
        
        assertThrows(IllegalArgumentException.class, () -> {
            repository.save(invalidEntity);
        }, "Saving an entity with a null ID should throw IllegalArgumentException.");
    }
    
    // --- Tests for findById(String id) ---

    @Test
    void findById_EntityExists_ShouldReturnOptionalWithEntity() {
        Entity existing = createTestEntity("E_FIND");
        repository.save(existing);

        Optional<Entity> found = repository.findById("E_FIND");
        
        assertTrue(found.isPresent());
        assertEquals(existing.getId(), found.get().getId());
    }

    @Test
    void findById_EntityDoesNotExist_ShouldReturnEmptyOptional() {
        Optional<Entity> found = repository.findById("NON_EXISTENT");
        
        assertTrue(found.isEmpty(), "Should return empty Optional for non-existent ID.");
    }
    
    // --- Tests for deleteById(String id) ---

    @Test
    void deleteById_EntityExists_ShouldSucceedAndRemoveEntity() throws EntityNotFoundException {
        Entity existing = createTestEntity("E_DELETE");
        repository.save(existing);
        
        // Perform deletion and ensure no exception is thrown
        assertDoesNotThrow(() -> repository.deleteById("E_DELETE"));
        
        // Verify entity is gone by trying to find it
        assertTrue(repository.findById("E_DELETE").isEmpty(), "Entity should be gone after deletion.");
    }

    @Test
    void deleteById_EntityDoesNotExist_ShouldThrowEntityNotFoundException() {
        // Verify that trying to delete an ID that doesn't exist throws the expected exception
        assertThrows(EntityNotFoundException.class, () -> {
            repository.deleteById("NON_EXISTENT_DELETE");
        }, "Deleting a non-existent entity should throw EntityNotFoundException.");
    }
    
    // --- Tests for deleteAll() ---

    @Test
    void deleteAll_MultipleEntities_ShouldClearRepository() {
        repository.save(createTestEntity("DEL_1"));
        repository.save(createTestEntity("DEL_2"));
        
        // Ensure data exists before calling deleteAll (using the internal map for verification)
        assertEquals(2, repository.mockDatabase.size()); 

        repository.deleteAll();
        
        // Verify repository is empty
        assertEquals(0, repository.mockDatabase.size(), "Repository size should be zero after deleteAll.");
        assertTrue(repository.findById("DEL_1").isEmpty());
    }
}
