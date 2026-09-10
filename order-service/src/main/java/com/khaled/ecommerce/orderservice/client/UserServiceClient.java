package com.khaled.ecommerce.orderservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class UserServiceClient {

    private final RestClient restClient;

    public UserServiceClient(@Value("${services.user-service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public UserClientResponse getUser(Long userId) {
        try {
            return restClient.get()
                    .uri("/api/users/{id}", userId)
                    .retrieve()
                    .body(UserClientResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new UserNotFoundException(userId);
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("User service", e);
        }
    }
}