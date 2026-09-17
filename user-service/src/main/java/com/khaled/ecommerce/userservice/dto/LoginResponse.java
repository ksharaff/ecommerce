package com.khaled.ecommerce.userservice.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,      // always "Bearer" - tells the client how to format the header
        long expiresInSeconds, // lets a client refresh proactively instead of waiting for a 401
        UserResponse user
) {}