
package com.example.caching.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Represents a cached entity.
 * It is assumed to be immutable once created for simplicity in caching, 
 * but mutability can be handled by the service if required.
 */
@Data // Generates getters, setters, toString, equals, and hashCode
@NoArgsConstructor
@AllArgsConstructor
public class Entity {
    private String id;
    private String data; // Example field for content

    /**
     * Required method as per assumption: returns a unique identifier for the object.
     * @return The unique ID of the entity.
     */
    public String getId() {
        return id;
    }
}