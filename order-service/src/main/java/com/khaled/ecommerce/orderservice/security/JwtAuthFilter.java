package com.khaled.ecommerce.orderservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    // A filter runs BEFORE any controller, on every request. OncePerRequestFilter guarantees
    // it runs exactly once even if the request gets internally forwarded.

    private final JwtService jwtService;
    private final AuthenticatedUser authenticatedUser;

    public JwtAuthFilter(JwtService jwtService, AuthenticatedUser authenticatedUser) {
        this.jwtService = jwtService;
        this.authenticatedUser = authenticatedUser;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7); // strip "Bearer " - 7 characters
            try {
                authenticatedUser.setUserId(jwtService.extractUserId(token));
            } catch (InvalidTokenException e) {
                // Deliberately NOT rejecting the request here. The filter's job is to identify
                // the caller when possible; deciding whether a particular endpoint REQUIRES
                // one belongs to the controller. A GET of a public product list shouldn't 401
                // just because someone sent a stale token.
            }
        }

        filterChain.doFilter(request, response); // hand off to the next filter, then the controller
    }
}