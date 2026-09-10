package com.khaled.ecommerce.orderservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ProductServiceClient {

    private final RestClient restClient;

    // @Value pulls one property straight into the constructor - simpler than a full
    // @ConfigurationProperties class when there's only one or two values to configure.
    public ProductServiceClient(@Value("${services.product-service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public ProductClientResponse getProduct(Long productId) {
        try {
            return restClient.get()
                    .uri("/api/products/{id}", productId)
                    .retrieve() // execute the call, prepare to read the response
                    .body(ProductClientResponse.class); // deserialize the JSON straight into our own DTO
        } catch (HttpClientErrorException.NotFound e) {
            // product-service's own GlobalExceptionHandler really did return a 404 - it genuinely doesn't exist
            throw new ProductNotFoundException(productId);
        } catch (RestClientException e) {
            // Connection refused, timeout, 500 from the other side, DNS failure - we don't know WHY,
            // only that we couldn't get a trustworthy answer.
            throw new ServiceUnavailableException("Product service", e);
        }
    }
}