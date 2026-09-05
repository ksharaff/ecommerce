package com.khaled.ecommerce.productservice.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.khaled.ecommerce.productservice.model.Product;
import com.khaled.ecommerce.productservice.repository.ProductRepository;

@Service
public class ProductService {
    
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // Create Product
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    public Product getProduct(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    public List<Product> listAll() {
        return productRepository.findAll();
    }

    public List<Product> listByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    public Product updateStock(Long id, Integer newQuantity){
        if(newQuantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");

        }
        Product product = getProduct(id);
        product.setStockQuantity(newQuantity);
        return productRepository.save(product);
    }

    public void deleteProduct(Long id) {
        if(!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteById(id);
    }
}
