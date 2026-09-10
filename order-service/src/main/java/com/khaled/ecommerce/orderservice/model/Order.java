package com.khaled.ecommerce.orderservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private Long userId; // just a number - the real User lives in a completely different database

    @Enumerated(EnumType.STRING) // stores "PENDING" as text, not 0/1/2 - readable directly in the DB,
    @Column(nullable = false, length = 20) // and safe if you ever reorder the enum's declared values
    private OrderStatus status;

    @NotNull
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount; // computed once at creation - a snapshot, never recalculated from live prices later

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    // This one's a REAL JPA relationship - Order and OrderItem share order-db, so a genuine foreign key applies.
    // cascade = ALL: saving/deleting an Order automatically saves/deletes its items - you never manage OrderItem directly.
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Order() {
    }

    public Order(Long userId) {
        this.userId = userId;
        this.status = OrderStatus.PENDING;
        this.totalAmount = BigDecimal.ZERO;
    }

    // The ONLY way items get added - keeps both sides of the relationship and the running total
    // consistent together, instead of trusting every caller to update three things correctly by hand.
    public void addItem(OrderItem item) {
        items.add(item);
        item.assignOrder(this);
        this.totalAmount = this.totalAmount.add(
                item.getUnitPriceAtOrderTime().multiply(BigDecimal.valueOf(item.getQuantity())));
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; } // legitimately mutable - order status changes over its lifecycle
    public BigDecimal getTotalAmount() { return totalAmount; }
    public List<OrderItem> getItems() { return items; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}