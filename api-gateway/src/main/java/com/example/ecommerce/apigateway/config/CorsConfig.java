package com.example.ecommerce.apigateway.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Local dev origins only. In production this would be your actual domain -
        // never "*" on an API that accepts credentials, since that defeats the point of CORS.
        //
        // The storefront is served on 3001, not 3000: Grafana's default port is 3000 and it
        // claims that port in docker-compose, so the two collided whenever the full stack was
        // up. Grafana has a convention behind its port and the static frontend does not, so
        // the frontend moved. This list is why it could not move unilaterally - an origin the
        // gateway does not allowlist gets every response blocked by the browser.
        config.setAllowedOrigins(List.of("http://localhost:3001", "http://127.0.0.1:3001"));
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setMaxAge(3600L); // browsers cache the preflight result for an hour

        // The browser sends an OPTIONS "preflight" request before any non-simple call
        // (anything with an Authorization header qualifies). Spring handles answering it
        // automatically once this filter is registered - but your gateway's JWT filter must
        // not reject it, which is why OPTIONS is in the allowed methods above.

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}