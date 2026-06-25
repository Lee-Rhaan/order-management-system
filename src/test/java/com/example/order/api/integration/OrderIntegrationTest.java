package com.example.order.api.integration;

import com.example.order.model.Order;
import com.example.order.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Order Management System.
 *
 * <p>Validates end-to-end flows with real database (H2 in-memory) and Spring context.
 * Tests both API and UI workflows.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@TestPropertySource("classpath:application-junit.properties")
class OrderIntegrationTest {

    /**
     * REST client for HTTP calls.
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * JSON serialization helper.
     */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Direct database access.
     */
    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    /**
     * Test complete flow: create order via API, fetch, update status.
     */
    @Test
    void testCompleteOrderWorkflow() throws Exception {
        // Step 1: Create order
        String createRequestJson = objectMapper.writeValueAsString(
                new com.example.order.dto.OrderCreateRequest(
                        "Alice Johnson", "Laptop", 2, BigDecimal.valueOf(899.99)
                )
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("NEW"));

        // Step 2: Fetch all orders
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].customerName").value("Alice Johnson"));

        // Step 3: Update status
        mockMvc.perform(patch("/api/orders/1/status")
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Step 4: Verify in database
        Order updated = orderRepository.findById(1L).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(Order.Status.COMPLETED);
    }

    /**
     * Test create via form submission.
     */
    @Test
    void testCreateOrderViaForm() throws Exception {
        mockMvc.perform(post("/orders")
                        .param("customerName", "Bob Smith")
                        .param("itemName", "Mouse")
                        .param("quantity", "5")
                        .param("unitPrice", "25.00")
                        .param("status", "NEW"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/view"));

        assertThat(orderRepository.count()).isEqualTo(1);
        Order saved = orderRepository.findAll().get(0);
        assertThat(saved.getCustomerName()).isEqualTo("Bob Smith");
        assertThat(saved.getItemName()).isEqualTo("Mouse");
    }

    /**
     * Test search functionality.
     */
    @Test
    void testSearchOrders() throws Exception {
        // Create multiple orders
        Order order1 = Order.builder()
                .customerName("Alice")
                .itemName("Laptop")
                .quantity(1)
                .unitPrice(BigDecimal.valueOf(899.99))
                .status(Order.Status.NEW)
                .createdAt(LocalDateTime.now())
                .build();

        Order order2 = Order.builder()
                .customerName("Bob")
                .itemName("Mouse")
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(25.00))
                .status(Order.Status.NEW)
                .createdAt(LocalDateTime.now())
                .build();

        orderRepository.save(order1);
        orderRepository.save(order2);

        // Search for Alice
        mockMvc.perform(get("/api/orders/search")
                        .param("customerName", "Alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].customerName").value("Alice"));
    }

    /**
     * Test analytics: status count.
     */
    @Test
    void testAnalyticsStatusCount() throws Exception {
        // Create orders with different statuses
        for (int i = 0; i < 3; i++) {
            Order order = Order.builder()
                    .customerName("Customer " + i)
                    .itemName("Item " + i)
                    .quantity(i + 1)
                    .unitPrice(BigDecimal.valueOf(100.00))
                    .status(i == 0 ? Order.Status.NEW : Order.Status.COMPLETED)
                    .createdAt(LocalDateTime.now())
                    .build();
            orderRepository.save(order);
        }

        mockMvc.perform(get("/api/orders/analytics/status-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.NEW").value(1))
                .andExpect(jsonPath("$.COMPLETED").value(2));
    }

    /**
     * Test analytics: completed revenue.
     */
    @Test
    void testAnalyticsCompletedRevenue() throws Exception {
        // Create orders
        Order completed1 = Order.builder()
                .customerName("Alice")
                .itemName("Laptop")
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(500.00))
                .status(Order.Status.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();

        Order completed2 = Order.builder()
                .customerName("Bob")
                .itemName("Mouse")
                .quantity(1)
                .unitPrice(BigDecimal.valueOf(50.00))
                .status(Order.Status.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();

        Order pending = Order.builder()
                .customerName("Charlie")
                .itemName("Keyboard")
                .quantity(1)
                .unitPrice(BigDecimal.valueOf(100.00))
                .status(Order.Status.NEW)
                .createdAt(LocalDateTime.now())
                .build();

        orderRepository.save(completed1);
        orderRepository.save(completed2);
        orderRepository.save(pending);

        mockMvc.perform(get("/api/orders/analytics/completed-revenue"))
                .andExpect(status().isOk())
                // (2 * 500) + (1 * 50) = 1050
                .andExpect(jsonPath("$").value(1050.00));
    }

    /**
     * Test UI order viewing.
     */
    @Test
    void testViewOrdersUI() throws Exception {
        Order order = Order.builder()
                .customerName("Alice")
                .itemName("Laptop")
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(899.99))
                .status(Order.Status.NEW)
                .createdAt(LocalDateTime.now())
                .build();
        orderRepository.save(order);

        mockMvc.perform(get("/orders/view"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attributeExists("orders", "totalOrders", "totalRevenue"));
    }

    /**
     * Test validation: customer name cannot be blank.
     */
    @Test
    void testValidation_BlankCustomerName() throws Exception {
        String invalidRequest = objectMapper.writeValueAsString(
                new com.example.order.dto.OrderCreateRequest(
                        "", "Laptop", 2, BigDecimal.valueOf(899.99)
                )
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='customerName')]").exists());
    }

    /**
     * Test validation: quantity must be positive.
     */
    @Test
    void testValidation_NegativeQuantity() throws Exception {
        String invalidRequest = objectMapper.writeValueAsString(
                new com.example.order.dto.OrderCreateRequest(
                        "Alice", "Laptop", -5, BigDecimal.valueOf(899.99)
                )
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='quantity')]").exists());
    }

    /**
     * Test 404 on non-existent order.
     */
    @Test
    void testNotFound() throws Exception {
        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    @Test
    void testCreateOrderViaForm_InvalidUnitPriceType() throws Exception {
        mockMvc.perform(post("/orders")
                        .param("customerName", "Bob Smith")
                        .param("itemName", "Mouse")
                        .param("quantity", "5")
                        .param("unitPrice", "abc")
                        .param("status", "NEW"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("unitPrice")));
    }

    @Test
    void testCreateOrderViaForm_MissingQuantity() throws Exception {
        mockMvc.perform(post("/orders")
                        .param("customerName", "Bob Smith")
                        .param("itemName", "Mouse")
                        .param("unitPrice", "25.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("Missing required parameter 'quantity'")));
    }

    @Test
    void testCreateOrderViaForm_ValidationFailure() throws Exception {
        mockMvc.perform(post("/orders")
                        .param("customerName", "")
                        .param("itemName", "")
                        .param("quantity", "0")
                        .param("unitPrice", "0.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }
}