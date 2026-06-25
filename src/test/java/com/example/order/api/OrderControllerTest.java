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

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link OrderController}.
 *
 * <p>Tests both REST endpoints and Thymeleaf UI endpoints using MockMvc.
 * Service layer is mocked to isolate controller logic.</p>
 */
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    /**
     * MockMvc instance for simulating HTTP requests.
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * JSON serialization helper.
     */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Mocked service layer.
     */
    @MockBean
    private OrderService orderService;

    /**
     * Sample order for test data.
     */
    private Order sampleOrder;

    /**
     * Sample create request.
     */
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

    /* ========== UI ENDPOINTS ========== */

    /**
     * Test GET / returns index template.
     */
    @Test
    void testGetHome() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    /**
     * Test POST /orders creates order and redirects to /orders/view.
     */
    @Test
    void testCreateOrderFromForm_Success() throws Exception {
        when(orderService.createOrder(any(OrderCreateRequest.class)))
                .thenReturn(sampleOrder);

        mockMvc.perform(post("/orders")
                        .param("customerName", "Alice Johnson")
                        .param("productName", "Laptop")
                        .param("quantity", "2")
                        .param("unitPrice", "899.99")
                        .param("status", "NEW"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/view"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(orderService, times(1)).createOrder(any(OrderCreateRequest.class));
    }

    /**
     * Test POST /orders with status override.
     */
    @Test
    void testCreateOrderFromForm_WithStatusOverride() throws Exception {
        when(orderService.createOrder(any(OrderCreateRequest.class)))
                .thenReturn(sampleOrder);
        when(orderService.updateStatus(1L, Order.Status.PROCESSING))
                .thenReturn(Optional.of(sampleOrder));

        mockMvc.perform(post("/orders")
                        .param("customerName", "Alice Johnson")
                        .param("productName", "Laptop")
                        .param("quantity", "2")
                        .param("unitPrice", "899.99")
                        .param("status", "PROCESSING"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/view"));

        verify(orderService).updateStatus(1L, Order.Status.PROCESSING);
    }

    /**
     * Test POST /orders with exception redirects to home with error message.
     */
    @Test
    void testCreateOrderFromForm_Failure() throws Exception {
        when(orderService.createOrder(any(OrderCreateRequest.class)))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(post("/orders")
                        .param("customerName", "Alice Johnson")
                        .param("productName", "Laptop")
                        .param("quantity", "2")
                        .param("unitPrice", "899.99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    /**
     * Test GET /orders/view returns orders template with summary metrics.
     */
    @Test
    void testViewOrders_All() throws Exception {
        when(orderService.getAllOrders())
                .thenReturn(List.of(sampleOrder));

        mockMvc.perform(get("/orders/view"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attributeExists(
                        "orders", "totalOrders", "totalQuantity", "totalRevenue", "completedOrders"
                ))
                .andExpect(model().attribute("totalOrders", 1))
                .andExpect(model().attribute("totalQuantity", 2));

        verify(orderService).getAllOrders();
    }

    /**
     * Test GET /orders/view?orderId=1 returns single order.
     */
    @Test
    void testViewOrders_SingleOrder() throws Exception {
        when(orderService.getOrderById(1L))
                .thenReturn(Optional.of(sampleOrder));

        mockMvc.perform(get("/orders/view").param("orderId", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attribute("totalOrders", 1));

        verify(orderService).getOrderById(1L);
    }

    /**
     * Test GET /orders/view?orderId=999 (not found) throws ResourceNotFoundException.
     */
    @Test
    void testViewOrders_OrderNotFound() throws Exception {
        when(orderService.getOrderById(999L))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/orders/view").param("orderId", "999"))
                .andExpect(status().isOk())
                .andExpect(view().name("error"));
    }

    /* ========== REST ENDPOINTS ========== */

    /**
     * Test POST /api/orders creates order with JSON body.
     */
    @Test
    void testCreateOrderApi_Success() throws Exception {
        when(orderService.createOrder(any(OrderCreateRequest.class)))
                .thenReturn(sampleOrder);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.customerName").value("Alice Johnson"))
                .andExpect(jsonPath("$.quantity").value(2));

        verify(orderService).createOrder(any(OrderCreateRequest.class));
    }

    /**
     * Test POST /api/orders with invalid data returns 400 Bad Request.
     */
    @Test
    void testCreateOrderApi_InvalidData() throws Exception {
        String invalidRequest = "{\"customerName\": \"\", \"itemName\": \"\", \"quantity\": -1, \"unitPrice\": \"invalid\"}";

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThan(0))));
    }

    /**
     * Test GET /api/orders returns all orders.
     */
    @Test
    void testGetOrdersApi() throws Exception {
        when(orderService.getAllOrders())
                .thenReturn(List.of(sampleOrder));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].customerName").value("Alice Johnson"));

        verify(orderService).getAllOrders();
    }

    /**
     * Test GET /api/orders/{id} returns single order.
     */
    @Test
    void testGetOrderByIdApi() throws Exception {
        when(orderService.getOrderById(1L))
                .thenReturn(Optional.of(sampleOrder));

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.customerName").value("Alice Johnson"));

        verify(orderService).getOrderById(1L);
    }

    /**
     * Test GET /api/orders/{id} with non-existent id returns 404.
     */
    @Test
    void testGetOrderByIdApi_NotFound() throws Exception {
        when(orderService.getOrderById(999L))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        verify(orderService).getOrderById(999L);
    }

    /**
     * Test GET /api/orders/search?customerName=alice returns matching orders.
     */
    @Test
    void testSearchOrdersApi() throws Exception {
        when(orderService.searchByCustomerName("Alice"))
                .thenReturn(List.of(sampleOrder));

        mockMvc.perform(get("/api/orders/search").param("customerName", "Alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].customerName").value("Alice Johnson"));

        verify(orderService).searchByCustomerName("Alice");
    }

    /**
     * Test GET /api/orders/search without param returns all orders.
     */
    @Test
    void testSearchOrdersApi_NoParam() throws Exception {
        when(orderService.getAllOrders())
                .thenReturn(List.of(sampleOrder));

        mockMvc.perform(get("/api/orders/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(orderService).getAllOrders();
    }

    /**
     * Test PATCH /api/orders/{id}/status updates status.
     */
    @Test
    void testUpdateStatusApi() throws Exception {
        Order completedOrder = Order.builder()
                .id(1L)
                .customerName("Alice Johnson")
                .itemName("Laptop")
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(899.99))
                .status(Order.Status.COMPLETED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(orderService.updateStatus(1L, Order.Status.COMPLETED))
                .thenReturn(Optional.of(completedOrder));

        mockMvc.perform(patch("/api/orders/1/status")
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(orderService).updateStatus(1L, Order.Status.COMPLETED);
    }

    /**
     * Test GET /api/orders/analytics/status-count returns status distribution.
     */
    @Test
    void testStatusCountApi() throws Exception {
        Map<Order.Status, Long> statusCounts = Map.of(
                Order.Status.NEW, 1L,
                Order.Status.COMPLETED, 2L,
                Order.Status.PROCESSING, 1L
        );

        when(orderService.countByStatus())
                .thenReturn(statusCounts);

        mockMvc.perform(get("/api/orders/analytics/status-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.NEW").value(1))
                .andExpect(jsonPath("$.COMPLETED").value(2))
                .andExpect(jsonPath("$.PROCESSING").value(1));

        verify(orderService).countByStatus();
    }

    /**
     * Test GET /api/orders/analytics/completed-revenue calculates revenue.
     */
    @Test
    void testCompletedRevenueApi() throws Exception {
        when(orderService.calculateCompletedRevenue())
                .thenReturn(BigDecimal.valueOf(1799.98));

        mockMvc.perform(get("/api/orders/analytics/completed-revenue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1799.98));

        verify(orderService).calculateCompletedRevenue();
    }

    @Test
    void testCreateOrderFromForm_InvalidQuantityType() throws Exception {
        mockMvc.perform(post("/orders")
                        .param("customerName", "Alice")
                        .param("productName", "Laptop")
                        .param("quantity", "abc")
                        .param("unitPrice", "899.99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("quantity")))
                .andExpect(jsonPath("$.message", containsString("Expected Integer")));
    }

    @Test
    void testCreateOrderFromForm_InvalidUnitPriceType() throws Exception {
        mockMvc.perform(post("/orders")
                        .param("customerName", "Alice")
                        .param("productName", "Laptop")
                        .param("quantity", "2")
                        .param("unitPrice", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("unitPrice")))
                .andExpect(jsonPath("$.message", containsString("Expected BigDecimal")));
    }

    @Test
    void testCreateOrderFromForm_InvalidStatusEnum() throws Exception {
        mockMvc.perform(post("/orders")
                        .param("customerName", "Alice")
                        .param("productName", "Laptop")
                        .param("quantity", "2")
                        .param("unitPrice", "899.99")
                        .param("status", "INVALID_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("status")));
    }

    @Test
    void testCreateOrderFromForm_MissingRequiredParam() throws Exception {
        mockMvc.perform(post("/orders")
                        .param("customerName", "Alice")
                        .param("productName", "Laptop")
                        // quantity missing
                        .param("unitPrice", "899.99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Missing required parameter 'quantity'")));
    }
}