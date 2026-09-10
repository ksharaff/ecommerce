package com.khaled.ecommerce.orderservice.client;

public class ProductNotFoundException extends RuntimeException{
    public ProductNotFoundException(Long productId) {
        super("Product with ID " + productId + " not found.");
    }
}
