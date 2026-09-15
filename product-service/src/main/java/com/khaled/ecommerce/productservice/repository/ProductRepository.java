package com.khaled.ecommerce.productservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.khaled.ecommerce.productservice.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long>{

    List<Product> findByCategory(String category);

    List<Product> findByNameContainingIgnoreCase(String name);

    // to_tsvector('english', ...) 
    // converts text into a searchable form — 
    // splits into words, lowercases, strips common noise words ("the", "a"), 
    // and reduces words to stems so "wireless" matches "wirelessly".
    
    // name || ' ' || coalesce(description, '') 
    // concatenates both fields so one query searches across them. 
    // coalesce matters: in SQL, concatenating anything with NULL yields NULL, 
    // so a product with no description would become invisible to search without it.
    
    // plainto_tsquery 
    // turns the user's raw input into a query safely — critically, 
    // it handles arbitrary user text without needing special-character escaping, unlike its pickier cousin to_tsquery.
    
    // @@ 
    // is Postgres's "does this document match this query" operator.
    
    // ts_rank(...) DESC 
    // orders by relevance rather than by id — a product with the term in its name 
    // ranks above one that merely mentions it in a description.
    @Query(value = """
            SELECT * FROM products
            WHERE to_tsvector('english', name || ' ' || coalesce(description, ''))
                  @@ plainto_tsquery('english', :searchTerm)
            ORDER BY ts_rank(
                to_tsvector('english', name || ' ' || coalesce(description, '')),
                plainto_tsquery('english', :searchTerm)
            ) DESC
            """, nativeQuery = true)
    List<Product> search(@Param("searchTerm") String searchTerm);
    
}
