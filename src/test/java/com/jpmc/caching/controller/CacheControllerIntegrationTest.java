package com.jpmc.caching.controller;

import com.example.caching.CachingServiceApplication;
import com.example.caching.model.Entity;
import com.example.caching.repository.DatabaseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(classes = CachingServiceApplication.class)
class CacheControllerIntegrationTest {

    private static final String API_PATH = "/api/v1/cache";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Inject the real repository for setup/cleanup in true integration tests
    @Autowired
    private DatabaseRepository databaseRepository; 

    @BeforeEach
    void setup() {
        // Clear all data before each test for isolation
        databaseRepository.deleteAll();
    }

    private Entity createEntity(String id) {
        return new Entity(id, "test-data-" + id);
    }

    @Test
    void addAndGetEntity_Success_CacheHit() throws Exception {
        Entity e1 = createEntity("IT_E1");

        // 1. ADD: Add the entity (Cache/DB should have it)
        mockMvc.perform(MockMvcRequestBuilders.post(API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(e1)))
                .andExpect(status().isOk());

        // 2. GET: Retrieve the entity (Cache Hit)
        mockMvc.perform(MockMvcRequestBuilders.get(API_PATH + "/{id}", "IT_E1"))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(e1)));
    }

    @Test
    void getEntity_CacheMissDBHit_Success() throws Exception {
        Entity e2 = createEntity("IT_E2_DB");
        
        // 1. Pre-load entity directly into DB (simulating a prior eviction/save)
        databaseRepository.save(e2); 

        // 2. GET: Retrieve the entity (Cache Miss, DB Hit, should promote to cache)
        mockMvc.perform(MockMvcRequestBuilders.get(API_PATH + "/{id}", "IT_E2_DB"))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(e2)));
    }
    
    @Test
    void getEntity_NotFound_Failure() throws Exception {
        // GET: Non-existent entity
        mockMvc.perform(MockMvcRequestBuilders.get(API_PATH + "/{id}", "IT_NONEXISTENT"))
                .andExpect(status().isNotFound()) // 404
                .andExpect(content().string(containsString("not found in cache or database")));
    }

    @Test
    void removeEntity_Success() throws Exception {
        String entityId = "IT_E3_REMOVE";
        Entity e3 = createEntity(entityId);
        
        // 1. Add to cache (ensures entity exists in DB/Cache prior to deletion)
        mockMvc.perform(MockMvcRequestBuilders.post(API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(e3)))
                .andExpect(status().isOk());
        
        // 2. REMOVE: Remove from cache and DB (Using the explicit /item/{id} path)
        mockMvc.perform(MockMvcRequestBuilders.delete(API_PATH + "/item/"+entityId, entityId))
        .andExpect(status().isOk());
        
        // 3. Verify removal (should now be 404 Not Found)
        mockMvc.perform(MockMvcRequestBuilders.get(API_PATH + "/2", entityId))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeAll_Success() throws Exception {
        // Add two entities
        mockMvc.perform(MockMvcRequestBuilders.post(API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createEntity("IT_R_1"))))
                .andExpect(status().isOk());
        databaseRepository.save(createEntity("IT_R_2_DB_ONLY"));
        
        // REMOVE ALL
        mockMvc.perform(MockMvcRequestBuilders.delete(API_PATH + "/all"))
                .andExpect(status().isNoContent()); // 204

        // Verify entities are gone
        mockMvc.perform(MockMvcRequestBuilders.get(API_PATH + "/IT_R_1"))
                .andExpect(status().isNotFound());
        mockMvc.perform(MockMvcRequestBuilders.get(API_PATH + "/IT_R_2_DB_ONLY"))
                .andExpect(status().isNotFound());
    }
}
