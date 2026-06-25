package com.example.order;

import com.example.order.controller.OrderController;
import com.example.order.repository.OrderRepository;
import com.example.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Application context integration tests.
 *
 * <p>Verifies that the Spring Boot application context loads correctly
 * and all primary beans are present and wired.</p>
 */
@SpringBootTest
@ActiveProfiles("junit")
@TestPropertySource("classpath:application-junit.properties")
class OrderApplicationTests {

    /**
     * Injected controller bean.
     */
    @Autowired
    private OrderController orderController;

    /**
     * Injected service bean.
     */
    @Autowired
    private OrderService orderService;

    /**
     * Injected repository bean.
     */
    @Autowired
    private OrderRepository orderRepository;

    /**
     * Test that application context loads without errors.
     */
    @Test
    void contextLoads() {
        assertThat(orderController).isNotNull();
    }

    /**
     * Test that all key beans are correctly wired.
     */
    @Test
    void allBeansAreLoaded() {
        assertThat(orderController).isNotNull();
        assertThat(orderService).isNotNull();
        assertThat(orderRepository).isNotNull();
    }
}