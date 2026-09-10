package com.khaled.ecommerce.orderservice.service;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(Long productId, Integer available, Integer requested) {
        super("Insufficient stock for product " + productId + ": requested " + requested + ", only " + available + " available");
    }
    
}
