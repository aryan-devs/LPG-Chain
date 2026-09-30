package com.lpgchain.exception;

/**
 * Exception thrown when an entity or resource (Vehicle, Delivery, Location, etc.)
 * cannot be found in the database. Maps to HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
