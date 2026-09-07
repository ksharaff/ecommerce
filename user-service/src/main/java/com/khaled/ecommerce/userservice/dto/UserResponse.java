package com.khaled.ecommerce.userservice.dto;

import java.time.Instant;

import com.khaled.ecommerce.userservice.model.User;

public record UserResponse(
    Long id,
    String email,
    String firstName,
    String lastName,
    Instant createdAt,
    Instant updatedAt
) {
    public static UserResponse fromUser(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
        // passwordHash never appears here — not filtered out, just never referenced.
        // The DTO makes leaking it structurally impossible, not just something you have to remember not to do.
    }
    
}
