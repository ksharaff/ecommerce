package com.khaled.ecommerce.orderservice.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // real relationship, same database as Order
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @NotNull
    @Column(nullable = false)
    private Long productId; // just a number again - the real Product lives in product-db, not here

    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer quantity;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPriceAtOrderTime; // snapshot of Product's price AT THE MOMENT this order was placed

    protected OrderItem() {
    }

    public OrderItem(Long productId, Integer quantity, BigDecimal unitPriceAtOrderTime) {
        this.productId = productId;
        this.quantity = quantity;
        this.unitPriceAtOrderTime = unitPriceAtOrderTime;
    }

    // Package-private, not public - only Order.addItem() should ever call this, so the two sides
    // of the relationship can never get out of sync with each other.
    void assignOrder(Order order) {
        this.order = order;
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getUnitPriceAtOrderTime() { return unitPriceAtOrderTime; }
}