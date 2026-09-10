package com.khaled.ecommerce.controller;

import com.khaled.ecommerce.orderservice.client.ProductNotFoundException;
import com.khaled.ecommerce.orderservice.client.ServiceUnavailableException;
import com.khaled.ecommerce.orderservice.client.UserNotFoundException;
import com.khaled.ecommerce.orderservice.controller.OrderController;
import com.khaled.ecommerce.orderservice.dto.CreateOrderRequest;
import com.khaled.ecommerce.orderservice.dto.OrderItemDto;
import com.khaled.ecommerce.orderservice.model.Order;
import com.khaled.ecommerce.orderservice.model.OrderItem;
import com.khaled.ecommerce.orderservice.service.InsufficientStockException;
import com.khaled.ecommerce.orderservice.service.OrderNotFoundException;
import com.khaled.ecommerce.orderservice.service.OrderService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;

    @MockitoBean
    private OrderService orderService;

    private Order sampleOrder() {
        Order order = new Order(1L);
        order.addItem(new OrderItem(10L, 2, new BigDecimal("29.99")));
        return order;
    }

    @Test
    void placeOrder_withValidRequest_returns201() throws Exception {
        when(orderService.placeOrder(any(), any())).thenReturn(sampleOrder());

        CreateOrderRequest request = new CreateOrderRequest(1L, List.of(new OrderItemDto(10L, 2)));

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    void placeOrder_withEmptyItemsList_returns400() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(1L, List.of()); // @NotEmpty should catch this

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void placeOrder_withZeroQuantityItem_returns400() throws Exception {
        // This is the test that would fail if @Valid were missing on the items list in
        // CreateOrderRequest - without it, this invalid nested object sails straight through untouched.
        CreateOrderRequest request = new CreateOrderRequest(1L, List.of(new OrderItemDto(10L, 0)));

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void placeOrder_whenUserNotFound_returns400() throws Exception {
        when(orderService.placeOrder(any(), any())).thenThrow(new UserNotFoundException(999L));

        CreateOrderRequest request = new CreateOrderRequest(999L, List.of(new OrderItemDto(10L, 1)));

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void placeOrder_whenProductNotFound_returns400() throws Exception {
        when(orderService.placeOrder(any(), any())).thenThrow(new ProductNotFoundException(999L));

        CreateOrderRequest request = new CreateOrderRequest(1L, List.of(new OrderItemDto(999L, 1)));

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void placeOrder_withInsufficientStock_returns409() throws Exception {
        when(orderService.placeOrder(any(), any())).thenThrow(new InsufficientStockException(10L, 5, 100));

        CreateOrderRequest request = new CreateOrderRequest(1L, List.of(new OrderItemDto(10L, 100)));

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void placeOrder_whenDependencyServiceDown_returns503() throws Exception {
        when(orderService.placeOrder(any(), any()))
                .thenThrow(new ServiceUnavailableException("Product service", new RuntimeException()));

        CreateOrderRequest request = new CreateOrderRequest(1L, List.of(new OrderItemDto(10L, 1)));

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void getOrder_whenNotFound_returns404() throws Exception {
        when(orderService.getOrder(999L)).thenThrow(new OrderNotFoundException(999L));

        mockMvc.perform(get("/api/orders/999")).andExpect(status().isNotFound());
    }
}
