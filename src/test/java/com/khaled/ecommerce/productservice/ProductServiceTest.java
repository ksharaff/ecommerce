package com.khaled.ecommerce.productservice;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.khaled.ecommerce.productservice.model.Product;
import com.khaled.ecommerce.productservice.repository.ProductRepository;
import com.khaled.ecommerce.productservice.service.ProductService;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {
    
    @Mock // a fake ProductRepoitory - no real database, no docker, no spring
    private ProductRepository productRepository;

    @InjectMocks // builds a real ProductService, passing the mock above to its constructor
    private ProductService productService;

    @Test 
    void createProduct_savesAndReturnsProduct() {
        Product product = new Product();
        product.setName("Mouse");
        product.setDescription("Wireless Mouse");
        product.setPrice(new BigDecimal("29.99"));
        product.setStockQuantity(100);
        product.setCategory("Electronics");

        when(productRepository.save(product).thenReturn(product));

        Product result = productService.createProduct(product);

        // check the result, and that the repo was actually used
        assertThat(result.getName()).isEqualTo("Mouse");
        verify(productRepository).save(product); // fails if save() was never called
    }

    @Test 
    void getProduct_whenExists_returnsProduct() {
        
    }
}
