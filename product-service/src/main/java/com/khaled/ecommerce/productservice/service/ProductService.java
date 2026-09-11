package com.khaled.ecommerce.productservice.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.khaled.ecommerce.productservice.model.Product;
import com.khaled.ecommerce.productservice.repository.ProductRepository;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ProductService {
    
    private final ProductRepository productRepository;

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

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

    @Transactional
    // @Transactional matters here in a way it didn't for the simple CRUD methods: this loops over
    // multiple products. Without it, a failure on item 3 would leave items 1 and 2 already
    // decremented - a half-applied order. With it, the whole batch commits or none of it does.
    public void decrementStockForOrder(Long orderId, List<StockDecrement> decrements) {
        for (StockDecrement decrement : decrements) {
            Product product = getProduct(decrement.productId()); // reuses the existing not-found check

            int newQuantity = product.getStockQuantity() - decrement.quantity();
            if (newQuantity < 0) {
                // Stock was sufficient when the order was placed, but isn't now - another order got
                // there first. This is a REAL consequence of eventual consistency, not a bug in the
                // code. Logging and skipping is the honest minimum; a production system would emit a
                // compensating event here to cancel the order and refund the payment (the "saga"
                // pattern). Worth knowing that's the name for it.
                log.warn("Insufficient stock for product {} on order {} - requested {}, available {}",
                        decrement.productId(), orderId, decrement.quantity(), product.getStockQuantity());
                continue;
            }

            product.setStockQuantity(newQuantity);
            productRepository.save(product);
            log.info("Decremented product {} by {} for order {}", decrement.productId(), decrement.quantity(), orderId);
        }
    }

    public record StockDecrement(Long productId, Integer quantity) {}
}
