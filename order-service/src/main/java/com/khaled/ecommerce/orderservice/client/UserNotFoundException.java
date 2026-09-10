package com.khaled.ecommerce.orderservice.client;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long userId) {
        super("User with ID " + userId + " not found.");
    }
    
}
