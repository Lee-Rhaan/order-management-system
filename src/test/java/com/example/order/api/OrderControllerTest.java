package com.example.order.api;

import com.example.order.controller.OrderController;
import com.example.order.dto.OrderCreateRequest;
import com.example.order.model.Order;
import com.example.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link OrderController}.
 *
 * <p>Tests both REST endpoints and Thymeleaf UI endpoints using MockMvc.
 * Service layer is mocked to isolate controller logic.</p>
 */
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    private Order sampleOrder;
    private OrderCreateRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleOrder = Order.builder()
                .id(1L)
                .customerName("Alice Johnson")
                .itemName("Laptop")
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(899.99))
                .status(Order.Status.NEW)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleRequest = OrderCreateRequest.builder()
                .customerName("Alice Johnson")
                .itemName("Laptop")
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(899.99))
                .build();
    }

    @Test
    void testGetHome() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    void testCreateOrderFromForm_Success() throws Exception {
        when(orderService.createOrder(any(OrderCreateRequest.class))).thenReturn(sampleOrder);

        mockMvc.perform(post("/orders")
                        .param("customerName", "Alice Johnson")
                        .param("itemName", "Laptop")
                        .param("quantity", "2")
                        .param("unitPrice", "899.99")
                        .param("status", "NEW"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/view"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(orderService).createOrder(any(OrderCreateRequest.class));
    }

    @Test
    void testCreateOrderFromForm_WithStatusOverride() throws Exception {
        when(orderService.createOrder(any(OrderCreateRequest.class))).thenReturn(sampleOrder);
        when(orderService.updateStatus(1L, Order.Status.PROCESSING)).thenReturn(Optional.of(sampleOrder));

        mockMvc.perform(post("/orders")
                        .param("customerName", "Alice Johnson")
                        .param("itemName", "Laptop")
                        .param("quantity", "2")
                        .param("unitPrice", "899.99")
                        .param("status", "PROCESSING"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/view"));

        verify(orderService).updateStatus(1L, Order.Status.PROCESSING);
    }

    @Test
    void testViewOrders_All() throws Exception {
        when(orderService.getAllOrders()).thenReturn(List.of(sampleOrder));

        mockMvc.perform(get("/orders/view"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attributeExists("orders", "totalOrders", "totalQuantity", "totalRevenue", "completedOrders"))
                .andExpect(model().attribute("totalOrders", 1L))
                .andExpect(model().attribute("totalQuantity", 2));

        verify(orderService).getAllOrders();
    }

    @Test
    void testViewOrders_SingleOrder() throws Exception {
        when(orderService.getOrderById(1L)).thenReturn(Optional.of(sampleOrder));

        mockMvc.perform(get("/orders/view").param("orderId", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attribute("totalOrders", 1L));

        verify(orderService).getOrderById(1L);
    }

    @Test
    void testViewOrders_OrderNotFound() throws Exception {
        when(orderService.getOrderById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/orders/view").param("orderId", "999"))
                .andExpect(status().isOk())
                .andExpect(view().name("error"));
    }

    @Test
    void testCreateOrderApi_Success() throws Exception {
        when(orderService.createOrder(any(OrderCreateRequest.class))).thenReturn(sampleOrder);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.customerName").value("Alice Johnson"))
                .andExpect(jsonPath("$.quantity").value(2));

        verify(orderService).createOrder(any(OrderCreateRequest.class));
    }

    @Test
    void testGetOrdersApi() throws Exception {
        when(orderService.getAllOrders()).thenReturn(List.of(sampleOrder));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1));

        verify(orderService).getAllOrders();
    }

    @Test
    void testGetOrderByIdApi() throws Exception {
        when(orderService.getOrderById(1L)).thenReturn(Optional.of(sampleOrder));

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void testSearchOrdersApi() throws Exception {
        when(orderService.searchByCustomerName("Alice")).thenReturn(List.of(sampleOrder));

        mockMvc.perform(get("/api/orders/search").param("customerName", "Alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void testUpdateStatusApi() throws Exception {
        Order completed = Order.builder()
                .id(1L)
                .customerName("Alice Johnson")
                .itemName("Laptop")
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(899.99))
                .status(Order.Status.COMPLETED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(orderService.updateStatus(1L, Order.Status.COMPLETED)).thenReturn(Optional.of(completed));

        mockMvc.perform(patch("/api/orders/1/status").param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void testStatusCountApi() throws Exception {
        when(orderService.countByStatus()).thenReturn(Map.of(
                Order.Status.NEW, 1L,
                Order.Status.COMPLETED, 2L,
                Order.Status.PROCESSING, 1L
        ));

        mockMvc.perform(get("/api/orders/analytics/status-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.NEW").value(1));
    }

    @Test
    void testCompletedRevenueApi() throws Exception {
        when(orderService.calculateCompletedRevenue()).thenReturn(BigDecimal.valueOf(1799.98));

        mockMvc.perform(get("/api/orders/analytics/completed-revenue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1799.98));
    }

    @Test
    void testCreateOrderFromForm_InvalidQuantityType() throws Exception {
        mockMvc.perform(post("/orders")
                        .param("customerName", "Alice")
                        .param("itemName", "Laptop")
                        .param("quantity", "abc")
                        .param("unitPrice", "899.99"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void testCreateOrderFromForm_InvalidUnitPriceType() throws Exception {
        mockMvc.perform(post("/orders")
                        .param("customerName", "Alice")
                        .param("itemName", "Laptop")
                        .param("quantity", "2")
                        .param("unitPrice", "abc"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void testCreateOrderFromForm_InvalidStatusEnum() throws Exception {
        mockMvc.perform(post("/orders")
                        .param("customerName", "Alice")
                        .param("itemName", "Laptop")
                        .param("quantity", "2")
                        .param("unitPrice", "899.99")
                        .param("status", "INVALID_STATUS"))
                .andExpect(status().isOk())
                .andExpect(view().name("error"));
    }

    @Test
    void testCreateOrderFromForm_MissingRequiredParam() throws Exception {
        mockMvc.perform(post("/orders")
                        .param("customerName", "Alice")
                        .param("itemName", "Laptop")
                        .param("unitPrice", "899.99"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void testUpdateOrderApi() throws Exception {
        Order updated = Order.builder()
                .id(1L)
                .customerName("Alice Updated")
                .itemName("Gaming Laptop")
                .quantity(3)
                .unitPrice(BigDecimal.valueOf(999.99))
                .status(Order.Status.NEW)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        OrderCreateRequest request = OrderCreateRequest.builder()
                .customerName("Alice Updated")
                .itemName("Gaming Laptop")
                .quantity(3)
                .unitPrice(BigDecimal.valueOf(999.99))
                .build();

        when(orderService.updateOrder(eq(1L), any(OrderCreateRequest.class))).thenReturn(Optional.of(updated));

        mockMvc.perform(put("/api/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Alice Updated"));
    }

    @Test
    void testDeleteOrderApi() throws Exception {
        when(orderService.deleteOrder(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/orders/1"))
                .andExpect(status().isNoContent());
    }
}