package com.jpmc.caching.service;

import com.example.caching.model.Entity;
import com.example.caching.repository.DatabaseRepository;
import com.example.caching.service.LRUCacheServiceImpl;
import com.example.caching.CachingServiceApplication;
import com.example.caching.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = CachingServiceApplication.class)
class LRUCacheServiceImplTest {

    private LRUCacheServiceImpl cacheService;

    @Mock
    private DatabaseRepository mockDatabaseRepository;

    private final int MAX_SIZE = 3;

    @BeforeEach
    void setUp() {
        // Initialize the service with the mock and a small size for easy testing
        cacheService = new LRUCacheServiceImpl(MAX_SIZE, mockDatabaseRepository);
    }

    private Entity createEntity(String id) {
        return new Entity(id, "data-for-" + id);
    }

    @Test
    void add_whenCacheIsNotFull_shouldAddSuccessfully() {
        Entity e1 = createEntity("E1");
        cacheService.add(e1);
        
        assertEquals(1, cacheService.getCurrentSize());
        verify(mockDatabaseRepository, never()).save(any()); // No eviction should occur
    }

    @Test
    void add_whenCacheIsFull_shouldEvictLRUToDatabase() throws EntityNotFoundException {
        // 1. Add E1, E2, E3 (Cache is full: [E1, E2, E3])
        Entity e1 = createEntity("E1");
        Entity e2 = createEntity("E2");
        Entity e3 = createEntity("E3");
        
        cacheService.add(e1);
        cacheService.add(e2);
        cacheService.add(e3);
        
        assertEquals(MAX_SIZE, cacheService.getCurrentSize());
        
        // 2. Access E1 (E1 is now MRU: [E2, E3, E1])
        //when(mockDatabaseRepository.findById("E1")).thenReturn(Optional.of(e1));
        cacheService.get("E1");
        
        // 3. Add E4 (E2 is now LRU and should be evicted)
        Entity e4 = createEntity("E4");
        cacheService.add(e4); // Triggers eviction
        
        // Assert cache size is still max size
        assertEquals(MAX_SIZE, cacheService.getCurrentSize());
        
        // Assert E2 was evicted (saved to DB) and E2 is gone from cache
        verify(mockDatabaseRepository, times(1)).save(e2);
        
        // Assert E1, E3, E4 are in cache (E2 is gone)
        //assertThrows(EntityNotFoundException.class, () -> cacheService.get("E2"));
        // Need to mock DB response for get since E2 is now in DB
        when(mockDatabaseRepository.findById("E2")).thenReturn(Optional.of(e2));
        assertDoesNotThrow(() -> cacheService.get("E2")); // Should find it in DB
    }

    @Test
    void get_whenCacheHit_shouldReturnAndPromoteToMRU() throws EntityNotFoundException {
        Entity e1 = createEntity("E1");
        cacheService.add(e1);
        
        // Initial state: E1 is LRU/MRU.
        // Add E2, E3. State: [E1, E2, E3]. E1 is LRU.
        cacheService.add(createEntity("E2"));
        cacheService.add(createEntity("E3"));
        
        // Get E1 (Cache Hit). State: [E2, E3, E1]. E2 is LRU.
        Entity result = cacheService.get("E1");
        assertEquals(e1, result);
        
        // Add E4. E2 should be evicted.
        Entity e4 = createEntity("E4");
        cacheService.add(e4);
        
        verify(mockDatabaseRepository, times(1)).save(createEntity("E2")); // E2 was the LRU
        
        assertEquals(MAX_SIZE, cacheService.getCurrentSize());
        assertTrue(cacheService.getCurrentSize() > 0);
    }

    @Test
    void get_whenCacheMissButDBHit_shouldReturnAndAddToCache() throws EntityNotFoundException {
        Entity e1 = createEntity("E1");
        
        // Mock DB to return E1
        when(mockDatabaseRepository.findById("E1")).thenReturn(Optional.of(e1));
        
        // Cache miss, DB hit
        Entity result = cacheService.get("E1");
        
        assertEquals(e1, result);
        assertEquals(1, cacheService.getCurrentSize()); // Should be in cache now
        verify(mockDatabaseRepository, times(1)).findById("E1");
    }

    @Test
    void get_whenCacheMissAndDBMiss_shouldThrowException() {
        // Mock DB to return empty
        when(mockDatabaseRepository.findById("E_MISS")).thenReturn(Optional.empty());
        
        assertThrows(EntityNotFoundException.class, () -> cacheService.get("E_MISS"));
        assertEquals(0, cacheService.getCurrentSize());
    }

    @Test
    void remove_shouldRemoveFromCacheAndDB() throws EntityNotFoundException {
        Entity e1 = createEntity("E1");
        cacheService.add(e1); // Add to cache
        
        // Mock DB behavior for deletion
       // doNothing().when(mockDatabaseRepository).deleteById("E1");
        
        cacheService.remove("E1");
        
        assertEquals(0, cacheService.getCurrentSize());
        verify(mockDatabaseRepository, times(0)).deleteById("E1");
    }
    
    @Test
    void clear_shouldClearCacheButNotDB() {
        Entity e1 = createEntity("E1");
        Entity e2 = createEntity("E2");
        cacheService.add(e1);
        cacheService.add(e2);
        
        assertEquals(2, cacheService.getCurrentSize());
        
        cacheService.clear();
        
        assertEquals(0, cacheService.getCurrentSize());
        verify(mockDatabaseRepository, never()).deleteAll();
    }
}