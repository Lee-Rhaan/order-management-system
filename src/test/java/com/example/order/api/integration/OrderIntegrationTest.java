package com.example.order.api.integration;

import com.example.order.dto.OrderCreateRequest;
import com.example.order.model.Order;
import com.example.order.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    /**
     * Test complete flow: create order via API, fetch, update status.
     *
     * <p>Uses dynamic order ID from creation response to avoid ID-sequence assumptions.</p>
     */
    @Test
    void testCompleteOrderWorkflow() throws Exception {
        String createRequestJson = objectMapper.writeValueAsString(
                new com.example.order.dto.OrderCreateRequest(
                        "Alice Johnson", "Laptop", 2, BigDecimal.valueOf(899.99)
                )
        );

        String createResponse = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long createdId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].customerName").value("Alice Johnson"));

        mockMvc.perform(patch("/api/orders/{id}/status", createdId)
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk());

        Order updated = orderRepository.findById(createdId).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(Order.Status.COMPLETED);
    }

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
    }

    @Test
    void testSearchOrders() throws Exception {
        orderRepository.save(Order.builder()
                .customerName("Alice")
                .itemName("Laptop")
                .quantity(1)
                .unitPrice(BigDecimal.valueOf(899.99))
                .status(Order.Status.NEW)
                .createdAt(LocalDateTime.now())
                .build());

        orderRepository.save(Order.builder()
                .customerName("Bob")
                .itemName("Mouse")
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(25.00))
                .status(Order.Status.NEW)
                .createdAt(LocalDateTime.now())
                .build());

        mockMvc.perform(get("/api/orders/search").param("customerName", "Alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void testValidation_BlankCustomerName_CurrentBehavior() throws Exception {
        String invalidRequest = objectMapper.writeValueAsString(
                new OrderCreateRequest("", "Laptop", 2, BigDecimal.valueOf(899.99))
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isOk());
    }

    @Test
    void testValidation_NegativeQuantity_CurrentBehavior() throws Exception {
        String invalidRequest = objectMapper.writeValueAsString(
                new OrderCreateRequest("Alice", "Laptop", -5, BigDecimal.valueOf(899.99))
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateOrderApi() throws Exception {
        Order existing = orderRepository.save(Order.builder()
                .customerName("Initial")
                .itemName("Initial Item")
                .quantity(1)
                .unitPrice(BigDecimal.valueOf(10.00))
                .status(Order.Status.NEW)
                .createdAt(LocalDateTime.now())
                .build());

        String updateJson = objectMapper.writeValueAsString(
                new OrderCreateRequest("Updated", "Updated Item", 3, BigDecimal.valueOf(75.50))
        );

        mockMvc.perform(put("/api/orders/" + existing.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk());

        Order updated = orderRepository.findById(existing.getId()).orElseThrow();
        assertThat(updated.getCustomerName()).isEqualTo("Updated");
    }

    @Test
    void testDeleteOrderApi() throws Exception {
        Order existing = orderRepository.save(Order.builder()
                .customerName("Delete")
                .itemName("Delete Item")
                .quantity(1)
                .unitPrice(BigDecimal.valueOf(10.00))
                .status(Order.Status.NEW)
                .createdAt(LocalDateTime.now())
                .build());

        mockMvc.perform(delete("/api/orders/" + existing.getId()))
                .andExpect(status().isNoContent());

        assertThat(orderRepository.findById(existing.getId())).isEmpty();
    }

    @Test
    void testUpdateOrderStatusFromUi() throws Exception {
        Order existing = orderRepository.save(Order.builder()
                .customerName("UI")
                .itemName("Item")
                .quantity(1)
                .unitPrice(BigDecimal.valueOf(20.00))
                .status(Order.Status.NEW)
                .createdAt(LocalDateTime.now())
                .build());

        mockMvc.perform(post("/orders/" + existing.getId() + "/status")
                        .param("status", "PROCESSING"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/view"));

        assertThat(orderRepository.findById(existing.getId()).orElseThrow().getStatus())
                .isEqualTo(Order.Status.PROCESSING);
    }

    @Test
    void testDeleteOrderFromUi() throws Exception {
        Order existing = orderRepository.save(Order.builder()
                .customerName("UI Delete")
                .itemName("Item")
                .quantity(1)
                .unitPrice(BigDecimal.valueOf(20.00))
                .status(Order.Status.NEW)
                .createdAt(LocalDateTime.now())
                .build());

        mockMvc.perform(post("/orders/" + existing.getId() + "/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/view"));

        assertThat(orderRepository.findById(existing.getId())).isEmpty();
    }
}