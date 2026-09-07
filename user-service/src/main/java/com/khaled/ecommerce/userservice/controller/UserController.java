package com.khaled.ecommerce.userservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.khaled.ecommerce.userservice.dto.LoginRequest;
import com.khaled.ecommerce.userservice.dto.RegisterRequest;
import com.khaled.ecommerce.userservice.dto.UserResponse;
import com.khaled.ecommerce.userservice.model.User;
import com.khaled.ecommerce.userservice.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    private static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getCreatedAt(), user.getUpdatedAt());
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.registerUser(request.email(), request.password(), request.firstName(), request.lastName());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(userService.getUser(id)));
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request) {
        // Real session/token issuance (JWT) comes later, on purpose - this just proves the
        // credentials are correct and hands back who you are.
        return ResponseEntity.ok(toResponse(userService.login(request.email(), request.password())));
    }
}