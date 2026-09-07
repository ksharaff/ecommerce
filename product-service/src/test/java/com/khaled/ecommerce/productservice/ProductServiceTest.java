package com.khaled.ecommerce.productservice;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.khaled.ecommerce.productservice.model.Product;
import com.khaled.ecommerce.productservice.repository.ProductRepository;
import com.khaled.ecommerce.productservice.service.ProductNotFoundException;
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

        when(productRepository.save(product)).thenReturn(product);

        Product result = productService.createProduct(product);

        // check the result, and that the repo was actually used
        assertThat(result.getName()).isEqualTo("Mouse");
        verify(productRepository).save(product); // fails if save() was never called
    }

    @Test  
    void getProduct_whenExists_returnsProduct() {
        Product existing = new Product();
        existing.setName("Mouse");
        existing.setDescription("Wireless Mouse");
        existing.setPrice(new BigDecimal("29.99"));
        existing.setStockQuantity(100);
        
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));

        Product result = productService.getProduct(1L);

        assertThat(result.getName()).isEqualTo("Mouse");
    }

    @Test 
    void getProduct_whenNotExists_throwException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // assertThatThrownBy runs the code and lets us assert on what it throws,
        // instead of cluttering the test with a manual try/catch
        assertThatThrownBy(() -> productService.getProduct(999L))
            .isInstanceOf(ProductNotFoundException.class)
            .hasMessageContaining("999");
}

    @Test 
    void updateStock_withValidQuantity_updatesAndSaves() {
        Product existing = new Product();
        existing.setName("Mouse");
        existing.setDescription("Wireless Mouse");
        existing.setPrice(new BigDecimal("29.99"));
        existing.setStockQuantity(100);
        existing.setCategory("Electronics");

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        // thenAnswer: just return whatever Product was passed in, simulating what save() really does

        Product result = productService.updateStock(1L, 50);

        assertThat(result.getStockQuantity()).isEqualTo(50);

    }

    @Test
    void updateStock_withNegativeQuantity_throwsException() {
        assertThatThrownBy(() -> productService.updateStock(1L, -5))
            .isInstanceOf(IllegalArgumentException.class);
        // No when(...) stubbing needed here - the negative check happens BEFORE the repository
        // is ever touched. This proves that, instead of just hoping it's true:
        verifyNoInteractions(productRepository);
    }

    @Test 
    void deleteProduct_whenNotExists_throwsException() {
        when(productRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> productService.deleteProduct(999L))
            .isInstanceOf(ProductNotFoundException.class);
        verify(productRepository, never()).deleteById(any()); // confirms delete was never attempted
    }
}
