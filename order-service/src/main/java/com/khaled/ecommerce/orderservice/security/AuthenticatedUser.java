package com.khaled.ecommerce.orderservice.security;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
// One instance per HTTP request, rather than the usual one-per-application. Spring creates
// a fresh one for each request and discards it after - which is exactly right for something
// holding "who is making THIS request."
public class AuthenticatedUser {
    private Long userId;

    public void setUserId(Long userId) { this.userId = userId; }

    public Long getUserId() {
        if (userId == null) {
            throw new InvalidTokenException(); // no token was validated for this request
        }
        return userId;
    }
}