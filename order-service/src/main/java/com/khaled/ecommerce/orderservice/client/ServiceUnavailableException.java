package com.khaled.ecommerce.orderservice.client;

public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String serviceName, Throwable cause) {
        super(serviceName + " service is currently unavailable. Please try again later.", cause);
    }
}
