package com.khaled.ecommerce.userservice.model;

import java.time.Instant;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Entity 
@Table(name = "users")
public class User {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank 
    @Email // validates basic email shape (something@something.something) - not a full RFC check, but catches real typos
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @NotBlank 
    @Column(nullable = false)
    private String passwordHash;

    @Column (nullable = false, length = 100)
    private String firstName;

    @Column (nullable = false, length = 100)
    private String lastName;

    @Column (nullable = false, updatable = false)
    private Instant createdAt;

    @Column (nullable = false)
    private Instant updatedAt;

    protected User() {

    }
    
    // Takes an ALREADY-HASHED password - hashing happens in the service layer, never in the entity.
    // The entity's only job is representing what gets stored; it shouldn't know or care how the hash was produced.
    public User(String email, String passwordHash, String firstName, String lastName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @PrePersist 
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate 
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    // Deliberately no setEmail() or setPasswordHash() yet - changing either of those safely needs its own
    // dedicated flow later (email change usually wants re-verification; password change needs the old password
    // confirmed first). A generic setter would make it too easy to skip that logic by accident.
}
    
