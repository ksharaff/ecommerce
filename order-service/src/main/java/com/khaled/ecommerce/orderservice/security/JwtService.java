package com.khaled.ecommerce.orderservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtService {

    private final SecretKey signingKey;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Returns the user id from a valid token, or throws if anything is wrong with it.
    public Long extractUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            // parseSignedClaims does ALL of this in one call: checks the signature is valid
            // for our key, checks "exp" hasn't passed, and rejects malformed tokens.
            // If any of that fails it throws - there is no "partially valid" outcome.

            return Long.valueOf(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException();
            // Deliberately collapsing every failure into one generic exception. An attacker
            // shouldn't learn whether a token was expired, forged, or malformed - same
            // reasoning as making unknown-email and wrong-password look identical at login.
        }
    }
}