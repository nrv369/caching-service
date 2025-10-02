package com.example.caching.service;

import com.example.caching.model.Entity;
import com.example.caching.repository.DatabaseRepository;
import com.example.caching.exception.EntityNotFoundException;
import com.example.caching.exception.CacheInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implementation of the CacheService using a thread-safe LRU strategy.
 * It uses a LinkedHashMap configured for access-order to track LRU.
 */
@Service
public class LRUCacheServiceImpl implements CacheService {

    private static final Logger log = LoggerFactory.getLogger(LRUCacheServiceImpl.class);

    private final int maxSize;
    private final DatabaseRepository databaseRepository;
    
    // LinkedHashMap is used as the core LRU mechanism:
    // accessOrder=true makes the iterator return entries in access order (LRU to MRU)
    private final Map<String, Entity> cache;

    // Use a ReadWriteLock for thread-safe access to the cache map
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Constructor for the caching service.
     * @param maxSize The configurable maximum number of elements in memory.
     * @param databaseRepository The persistence layer for eviction.
     */
    public LRUCacheServiceImpl(@Value("${cache.max-size:100}") int maxSize, 
                               DatabaseRepository databaseRepository) {
        if (maxSize <= 0) {
            log.error("Cache initialization failed: max-size must be greater than 0. Configured size: {}", maxSize);
            throw new CacheInitializationException("Cache max-size must be positive.");
        }
        this.maxSize = maxSize;
        this.databaseRepository = databaseRepository;
        
        // Initial capacity is calculated to avoid resizing until near max size
        int initialCapacity = (int) Math.ceil(maxSize / 0.75) + 1;

        // Custom LinkedHashMap implementation for LRU eviction logic
        this.cache = new LinkedHashMap<>(initialCapacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Entity> eldest) {
                // Check if removal is necessary (size > maxSize)
                if (size() > LRUCacheServiceImpl.this.maxSize) {
                    log.warn("Cache size exceeded max capacity of {}. Evicting LRU entity with ID: {}", 
                             LRUCacheServiceImpl.this.maxSize, eldest.getKey());
                    
                    // EVICTION LOGIC: Evict the least-used element to the database
                    databaseRepository.save(eldest.getValue());
                    return true; // Return true to remove the eldest entry from the map
                }
                return false; // Keep the element in the map
            }
        };
        
        log.info("LRUCacheServiceImpl initialized with max size: {}", maxSize);
    }

    /**
     * Adds an entity to the internal cache. If full, the LRU element is evicted to DB.
     */
    @Override
    public void add(Entity entity) {
        if (entity == null || entity.getId() == null) {
            throw new IllegalArgumentException("Entity and its ID must not be null.");
        }
        
        lock.writeLock().lock();
        try {
            // Note: Putting an existing key moves it to the MRU position (simulating a use/touch)
            cache.put(entity.getId(), entity);
            //databaseRepository.save(entity);
            log.debug("Added/Updated entity with ID '{}' in cache.", entity.getId());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes an entity from the internal cache and the database.
     */
    @Override
    public void remove(String id) throws EntityNotFoundException {
        if (id == null) {
             throw new IllegalArgumentException("Entity ID must not be null for removal.");
        }
        
        lock.writeLock().lock();
        try {
            // 1. Remove from cache (no exception needed if not found in cache)
            if (cache.remove(id) != null) {
                log.debug("Removed entity with ID '{}' from cache.", id);
            } else  {
                log.debug("Entity with ID '{}' not found in cache, checking DB for removal.", id);
                 // 2. Remove from database (will throw EntityNotFoundException if not found)
                 databaseRepository.deleteById(id);
            }            
            
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes all entities from the internal cache and the database.
     */
    @Override
    public void removeAll() {
        lock.writeLock().lock();
        try {
            cache.clear();
            log.info("Internal cache cleared by removeAll().");
            databaseRepository.deleteAll();
            log.warn("All entities removed from internal cache and database.");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Retrieves an entity, checking the cache first (hit), then the database (miss).
     */
    @Override
    public Entity get(String id) throws EntityNotFoundException {
        if (id == null) {
            throw new IllegalArgumentException("Entity ID must not be null for retrieval.");
        }
        
        // 1. Check cache (Read Lock)
        lock.readLock().lock();
        try {
            Entity cachedEntity = cache.get(id); // Access moves it to MRU position (LRU touch)
            if (cachedEntity != null) {
                log.debug("Cache hit for entity with ID '{}'.", id);
                return cachedEntity;
            }
        } finally {
            lock.readLock().unlock();
        }

        // 2. Cache Miss - Go to Database
        log.debug("Cache miss for entity with ID '{}'. Fetching from database.", id);
        
        Entity dbEntity = databaseRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Entity with ID " + id + " not found in cache or database."));
        
        log.info("DB hit for entity with ID '{}'. Promoting to cache.", id);

        // 3. Promote to cache (Write Lock)
        lock.writeLock().lock();
        try {
            cache.put(id, dbEntity); // Add to cache, potentially triggering eviction
        } finally {
            lock.writeLock().unlock();
        }

        return dbEntity;
    }

    /**
     * Clears the internal cache only.
     */
    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            cache.clear();
            log.warn("Internal cache cleared. Database entities unaffected.");
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Returns the configured maximum size.
     */
    @Override
    public int getMaxSize() {
        return maxSize;
    }
    
    /**
     * Utility method for testing/monitoring: Get current size of the cache.
     * @return Current number of elements in the cache.
     */
    public int getCurrentSize() {
        lock.readLock().lock();
        try {
            return cache.size();
        } finally {
            lock.readLock().unlock();
        }
    }
}