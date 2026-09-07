package com.khaled.ecommerce.userservice.controller;

import tools.jackson.databind.ObjectMapper;
import com.khaled.ecommerce.userservice.dto.LoginRequest;
import com.khaled.ecommerce.userservice.dto.RegisterRequest;
import com.khaled.ecommerce.userservice.model.User;
import com.khaled.ecommerce.userservice.service.EmailAlreadyExistsException;
import com.khaled.ecommerce.userservice.service.InvalidCredentialsException;
import com.khaled.ecommerce.userservice.service.UserService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    void register_withValidData_returns201AndNeverLeaksPasswordHash() throws Exception {
        User saved = new User("khaled@example.com", "hashed-value", "Khaled", "S");
        when(userService.registerUser(eq("khaled@example.com"), any(), eq("Khaled"), eq("S"))).thenReturn(saved);

        RegisterRequest request = new RegisterRequest("khaled@example.com", "supersecret1", "Khaled", "S");

        mockMvc.perform(post("/api/users/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("khaled@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        // That last line directly proves the security property, in code - not just trusting that
        // UserResponse was written correctly. If someone later "helpfully" adds passwordHash to the
        // DTO, this test fails immediately instead of silently shipping a leak.
    }

    @Test
    void register_withShortPassword_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("khaled@example.com", "short", "Khaled", "S");

        mockMvc.perform(post("/api/users/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_withDuplicateEmail_returns409() throws Exception {
        when(userService.registerUser(any(), any(), any(), any()))
                .thenThrow(new EmailAlreadyExistsException("khaled@example.com"));

        RegisterRequest request = new RegisterRequest("khaled@example.com", "supersecret1", "Khaled", "S");

        mockMvc.perform(post("/api/users/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void login_withBadCredentials_returns401() throws Exception {
        when(userService.login(any(), any())).thenThrow(new InvalidCredentialsException());

        LoginRequest request = new LoginRequest("khaled@example.com", "wrongpassword");

        mockMvc.perform(post("/api/users/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
