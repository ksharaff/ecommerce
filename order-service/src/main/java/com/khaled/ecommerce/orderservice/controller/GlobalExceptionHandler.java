package com.khaled.ecommerce.orderservice.controller;

import com.khaled.ecommerce.orderservice.client.ProductNotFoundException;
import com.khaled.ecommerce.orderservice.client.ServiceUnavailableException;
import com.khaled.ecommerce.orderservice.client.UserNotFoundException;
import com.khaled.ecommerce.orderservice.dto.ErrorResponse;
import com.khaled.ecommerce.orderservice.service.InsufficientStockException;
import com.khaled.ecommerce.orderservice.service.OrderNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler({UserNotFoundException.class, ProductNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleInvalidReference(RuntimeException ex) {
        // Worth noticing: internally this was a 404 FROM another service, but from Order's own
        // client's point of view, giving a userId/productId that doesn't exist is THEIR mistake -
        // a bad request to Order, not a "not found" on Order's own resource. The status changes
        // depending on whose API contract you're standing behind.
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(InsufficientStockException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
        // 409 Conflict - the request is well-formed, it conflicts with current stock levels
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(ServiceUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(message));
    }
}