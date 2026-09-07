package com.khaled.ecommerce.productservice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.khaled.ecommerce.productservice.dto.ProductResponse;
import com.khaled.ecommerce.productservice.dto.StockUpdateRequest;
import com.khaled.ecommerce.productservice.dto.ProductRequest;
import com.khaled.ecommerce.productservice.model.Product;
import com.khaled.ecommerce.productservice.service.ProductService;

import jakarta.validation.Valid;

@RestController 
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid  @RequestBody ProductRequest request) {
        // @RequestBody: deserialize the incoming JSON into a ProductRequest.
        // @Valid: before this method even runs, check every @NotBlank/@Positive/etc annotation on it —
        // if any fail, Spring throws MethodArgumentNotValidException before your code ever executes.
        Product product = new Product(
            request.name(), request.description(), request.price(),
            request.stockQuantity(), request.category()
        );
        Product saved = productService.createProduct(product);
        // 201 Created, not 200 OK — this endpoint made a new thing exist. That distinction matters to API consumers.
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.fromEntity(saved));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        // @PathVariable pulls {id} out of the URL, e.g. GET /api/products/7 -> id = 7
        Product product = productService.getProduct(id);
        return ResponseEntity.ok(ProductResponse.fromEntity(product));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> listProducts(
        @RequestParam(required = false) String category) {
        // @RequestParam reads a query string param: GET /api/products?category=Electronics
        // required=false means it's fine to omit — category will just be null
        List<Product> products = (category != null) 
        ? productService.listByCategory(category) 
        : productService.listAll();
        return ResponseEntity.ok(products.stream().map(ProductResponse::fromEntity).toList());
    }

    @PatchMapping("/{id}/stock") 
    public ResponseEntity<ProductResponse> updateStock(
        @PathVariable Long id, @Valid @RequestBody StockUpdateRequest request) {
    // PATCH, not PUT — we're updating one field, not replacing the whole resource
    Product updated = productService.updateStock(id, request.quantity());
    return ResponseEntity.ok(ProductResponse.fromEntity(updated));
}
    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build(); // 204 No content -- successful
    }

}   

