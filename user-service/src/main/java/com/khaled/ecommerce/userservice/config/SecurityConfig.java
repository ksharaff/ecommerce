package com.khaled.ecommerce.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
        // Registered as a Spring bean rather than "new BCryptPasswordEncoder()" scattered wherever needed -
        // that's what lets it be constructor-injected below, same idea as ProductRepository going into ProductService.
        // Coding against the PasswordEncoder INTERFACE (not the BCrypt class directly) means swapping hashing
        // algorithms later never touches the code that uses it - only this one line changes.
    }
}
