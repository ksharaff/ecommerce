package com.khaled.ecommerce.service;

import com.khaled.ecommerce.orderservice.client.*;
import com.khaled.ecommerce.orderservice.model.Order;
import com.khaled.ecommerce.orderservice.repository.OrderRepository;
import com.khaled.ecommerce.orderservice.service.InsufficientStockException;
import com.khaled.ecommerce.orderservice.service.OrderItemRequest;
import com.khaled.ecommerce.orderservice.service.OrderNotFoundException;
import com.khaled.ecommerce.orderservice.service.OrderService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock // three collaborators now, all faked the same way - the technique doesn't change with count
    private ProductServiceClient productServiceClient;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private OrderService orderService;

    @Test
    void placeOrder_withValidUserAndStock_savesOrderWithSnapshottedPrice() {
        when(userServiceClient.getUser(1L)).thenReturn(new UserClientResponse(1L, "khaled@example.com", "Khaled", "S"));
        when(productServiceClient.getProduct(10L)).thenReturn(new ProductClientResponse(10L, "Mouse", new BigDecimal("29.99"), 100));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.placeOrder(1L, List.of(new OrderItemRequest(10L, 2)));

        assertThat(result.getItems()).hasSize(1);
        // 2 * 29.99 = 59.98 - proves the snapshot price actually flowed into the total, not just "some number"
        assertThat(result.getTotalAmount()).isEqualByComparingTo("59.98");
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void placeOrder_whenUserDoesNotExist_failsFastBeforeTouchingProductsOrRepository() {
        when(userServiceClient.getUser(999L)).thenThrow(new UserNotFoundException(999L));

        assertThatThrownBy(() -> orderService.placeOrder(999L, List.of(new OrderItemRequest(10L, 1))))
                .isInstanceOf(UserNotFoundException.class);

        // Order matters: user gets checked BEFORE Product service or the repository are touched at all
        verifyNoInteractions(productServiceClient);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void placeOrder_whenProductDoesNotExist_propagatesWithoutSaving() {
        when(userServiceClient.getUser(1L)).thenReturn(new UserClientResponse(1L, "khaled@example.com", "Khaled", "S"));
        when(productServiceClient.getProduct(999L)).thenThrow(new ProductNotFoundException(999L));

        assertThatThrownBy(() -> orderService.placeOrder(1L, List.of(new OrderItemRequest(999L, 1))))
                .isInstanceOf(ProductNotFoundException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeOrder_withInsufficientStock_throwsAndNeverSaves() {
        when(userServiceClient.getUser(1L)).thenReturn(new UserClientResponse(1L, "khaled@example.com", "Khaled", "S"));
        when(productServiceClient.getProduct(10L)).thenReturn(new ProductClientResponse(10L, "Mouse", new BigDecimal("29.99"), 5));

        assertThatThrownBy(() -> orderService.placeOrder(1L, List.of(new OrderItemRequest(10L, 100))))
                .isInstanceOf(InsufficientStockException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeOrder_whenProductServiceIsDown_propagatesServiceUnavailableNotNotFound() {
        when(userServiceClient.getUser(1L)).thenReturn(new UserClientResponse(1L, "khaled@example.com", "Khaled", "S"));
        when(productServiceClient.getProduct(10L))
                .thenThrow(new ServiceUnavailableException("Product service", new RuntimeException("connection refused")));

        // Deliberately a DIFFERENT exception type than "product not found" - proving the distinction
        // we built survives all the way through OrderService, not just inside the client class.
        assertThatThrownBy(() -> orderService.placeOrder(1L, List.of(new OrderItemRequest(10L, 1))))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void getOrder_whenNotExists_throwsOrderNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(999L)).isInstanceOf(OrderNotFoundException.class);
    }
}