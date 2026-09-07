package com.khaled.ecommerce.productservice.repository;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.khaled.ecommerce.productservice.model.Product;

@DataJpaTest // loads ONLY JPA-related beans (repositories, entity manager) - fast, focused, not the whole app
@Testcontainers // tells JUnit to manage the lifecycle of any @Container fields below
@AutoConfigureTestDatabase(replace = Replace.NONE) // stop Spring swapping in embedded H2 over our real Postgres
public class ProductRepositoryTest {
    
    @Container 
    @ServiceConnection 
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private ProductRepository productRepository;

    @Test 
    void findByCategory_returnsMatchingProducts() {
        productRepository.save(new Product("Mouse", "Wireless Mouse", new BigDecimal("29.99"), 100, "Electronics"));
        productRepository.save(new Product("Keyboard", "Mechanical Keyboard", new BigDecimal("79.99"), 50, "Electronics"));

        List<Product> electronics = productRepository.findByCategory("Electronics");

        assertThat(electronics).hasSize(2);
        assertThat(electronics).extracting(Product::getName)
                .containsExactlyInAnyOrder("Mouse", "Keyboard");
    }

    @Test 
    void findByNameContainingIgnoreCase_matchesRegardlessOfCase() {
        productRepository.save(new Product("Mouse", "Wireless Mouse", new BigDecimal("29.99"), 100, "Electronics"));

        List<Product> results = productRepository.findByNameContainingIgnoreCase("mouse");

        assertThat(results).hasSize(1);
    }
}
