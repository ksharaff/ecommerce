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
import com.khaled.ecommerce.userservice.dto.LoginResponse;
import com.khaled.ecommerce.userservice.dto.RegisterRequest;
import com.khaled.ecommerce.userservice.dto.UserResponse;
import com.khaled.ecommerce.userservice.model.User;
import com.khaled.ecommerce.userservice.security.JwtService;
import com.khaled.ecommerce.userservice.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;

    public UserController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.registerUser(request.email(), request.password(), request.firstName(), request.lastName());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.fromUser(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(UserResponse.fromUser(userService.getUser(id)));
    }


    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.login(request.email(), request.password());
        String token = jwtService.generateToken(user.getId(), user.getEmail());

        return ResponseEntity.ok(new LoginResponse(
                token,
                "Bearer",
                jwtService.getExpirationMinutes() * 60,
                UserResponse.fromUser(user)));
    }
}