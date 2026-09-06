package com.khaled.ecommerce.productservice.controller;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.khaled.ecommerce.productservice.dto.ProductRequest;
import com.khaled.ecommerce.productservice.model.Product;
import com.khaled.ecommerce.productservice.service.ProductNotFoundException;
import com.khaled.ecommerce.productservice.service.ProductService;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(ProductController.class) // loads ONLY the web layer for this controller - no real service, no DB
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc; // simulates HTTP requests without opening a real network port

    @Autowired
    private ObjectMapper objectMapper; // Spring's JSON serializer, used here to build request bodies

    @MockitoBean // replaces the real ProductService in the test context with a fake, for this test only
    private ProductService productService;

    @Test
    void createProduct_withValidData_returns201() throws Exception {
        Product saved = new Product("Mouse", "desc", new BigDecimal("29.99"), 100, "Electronics");
        when(productService.createProduct(any(Product.class))).thenReturn(saved);

        ProductRequest request = new ProductRequest("Mouse", "desc", new BigDecimal("29.99"), 100, "Electronics");

        mockMvc.perform(post("/api/products")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Mouse"));
        // jsonPath reaches into the JSON response body, the same way you'd navigate a Java object graph
    }

    @Test
    void createProduct_withBlankName_returns400() throws Exception {
        // No stubbing needed - @Valid rejects this before productService is ever called
        ProductRequest badRequest = new ProductRequest("", "desc", new BigDecimal("29.99"), 100, "Electronics");

        mockMvc.perform(post("/api/products")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProduct_whenNotFound_returns404WithMessage() throws Exception {
        when(productService.getProduct(999L)).thenThrow(new ProductNotFoundException(999L));

        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with id: 999"));
    }
}