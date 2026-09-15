package com.khaled.ecommerce.productservice.service;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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

    @Cacheable(value = "products", key = "#id")
    // First call: runs the method, stores the result in Redis under key "products::<id>".
    // Every later call with the same id: returns from Redis, method body never executes.
    public Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    public List<Product> listAll() {
        return productRepository.findAll();
    }

    public List<Product> listByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    @CacheEvict(value = "products", key = "#id")
    // THE critical one. Without this, stock changes but the cache keeps serving the old number
    // for 10 minutes - and Order service would accept orders against inventory that's gone.
    public Product updateStock(Long id, Integer newQuantity){
        if(newQuantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");

        }
        Product product = getProduct(id);
        product.setStockQuantity(newQuantity);
        return productRepository.save(product);
    }

    @CacheEvict(value = "products", key = "#id")
    public void deleteProduct(Long id) {
        if(!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteById(id);
    }
    
    @CacheEvict(value = "products", allEntries = true)
    // allEntries clears the whole products cache rather than computing each key. Blunt, but correct -
    // and this runs once per order, not per request, so the cost is negligible.
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

        public List<Product> search(String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return List.of(); // blank search returns nothing, not everything
        }
        return productRepository.search(searchTerm);
        // Deliberately NOT @Cacheable: search terms are unbounded, so caching them would fill
        // Redis with thousands of single-use entries. Cache things read repeatedly, not things
        // typed once. Knowing what NOT to cache matters as much as knowing what to.
    }

    public record StockDecrement(Long productId, Integer quantity) {}
}
