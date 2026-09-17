package com.khaled.ecommerce.userservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMinutes;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-minutes}") long expirationMinutes) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        // hmacShaKeyFor throws immediately if the secret is under 256 bits - a deliberate
        // guardrail by the library, catching a weak key at startup rather than in production.
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(Long userId, String email) {
        Instant now = Instant.now();
        Instant expiry = now.plus(expirationMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(String.valueOf(userId))   // "sub" - the standard claim for "who this is about"
                .claim("email", email)             // a custom claim; fine, it's not sensitive
                .issuedAt(Date.from(now))          // "iat"
                .expiration(Date.from(expiry))     // "exp" - verification fails automatically after this
                .signWith(signingKey)
                .compact();
        // Deliberately NOT included: password hash, role data we don't have, anything private.
        // Anyone holding this token can base64-decode and read every claim in it.
    }

    public long getExpirationMinutes() {
        return expirationMinutes;
    }
}