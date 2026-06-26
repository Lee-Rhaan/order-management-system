package com.example.order.api.service;

import com.example.order.dto.OrderCreateRequest;
import com.example.order.model.Order;
import com.example.order.repository.OrderRepository;
import com.example.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OrderService}.
 *
 * <p>Tests business logic in isolation using mocked repository.
 * Demonstrates Java Streams operations.</p>
 */
class OrderServiceTest {

    /**
     * Service under test.
     */
    @InjectMocks
    private OrderService orderService;

    /**
     * Mocked repository.
     */
    @Mock
    private OrderRepository orderRepository;

    /**
     * Sample order for test data.
     */
    private Order order1;

    /**
     * Sample order for test data.
     */
    private Order order2;

    /**
     * Sample order for test data.
     */
    private Order order3;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        order1 = Order.builder()
                .id(1L)
                .customerName("Alice")
                .itemName("Laptop")
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(899.99))
                .status(Order.Status.NEW)
                .createdAt(LocalDateTime.now())
                .build();

        order2 = Order.builder()
                .id(2L)
                .customerName("Bob")
                .itemName("Mouse")
                .quantity(5)
                .unitPrice(BigDecimal.valueOf(25.00))
                .status(Order.Status.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();

        order3 = Order.builder()
                .id(3L)
                .customerName("Charlie")
                .itemName("Keyboard")
                .quantity(1)
                .unitPrice(BigDecimal.valueOf(120.00))
                .status(Order.Status.PROCESSING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Test createOrder saves and returns order.
     */
    @Test
    void testCreateOrder() {
        OrderCreateRequest request = OrderCreateRequest.builder()
                .customerName("  Alice  ")
                .itemName("Laptop")
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(899.99))
                .build();

        when(orderRepository.save(any(Order.class)))
                .thenReturn(order1);

        Order result = orderService.createOrder(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCustomerName()).isEqualTo("Alice");
        assertThat(result.getStatus()).isEqualTo(Order.Status.NEW);
        verify(orderRepository).save(any(Order.class));
    }

    /**
     * Test getAllOrders returns sorted list (newest first).
     */
    @Test
    void testGetAllOrders() {
        when(orderRepository.findAll())
                .thenReturn(List.of(order1, order2, order3));

        List<Order> result = orderService.getAllOrders();

        assertThat(result).hasSize(3);
        assertThat(result).extracting(Order::getId).containsExactlyInAnyOrder(1L, 2L, 3L);
        verify(orderRepository).findAll();
    }

    /**
     * Test getOrderById returns Optional.
     */
    @Test
    void testGetOrderById_Found() {
        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order1));

        Optional<Order> result = orderService.getOrderById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        verify(orderRepository).findById(1L);
    }

    /**
     * Test getOrderById returns empty when not found.
     */
    @Test
    void testGetOrderById_NotFound() {
        when(orderRepository.findById(999L))
                .thenReturn(Optional.empty());

        Optional<Order> result = orderService.getOrderById(999L);

        assertThat(result).isEmpty();
        verify(orderRepository).findById(999L);
    }

    /**
     * Test searchByCustomerName returns matching orders.
     */
    @Test
    void testSearchByCustomerName() {
        when(orderRepository.findByCustomerNameContainingIgnoreCase("Alice"))
                .thenReturn(List.of(order1));

        List<Order> result = orderService.searchByCustomerName("Alice");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomerName()).contains("Alice");
        verify(orderRepository).findByCustomerNameContainingIgnoreCase("Alice");
    }

    /**
     * Test searchByCustomerName with blank keyword returns empty.
     */
    @Test
    void testSearchByCustomerName_Blank() {
        List<Order> result = orderService.searchByCustomerName("   ");

        assertThat(result).isEmpty();
        verify(orderRepository, never()).findByCustomerNameContainingIgnoreCase(anyString());
    }

    /**
     * Test countByStatus demonstrates Streams grouping.
     */
    @Test
    void testCountByStatus() {
        when(orderRepository.findAll())
                .thenReturn(List.of(order1, order2, order3));

        Map<Order.Status, Long> result = orderService.countByStatus();

        assertThat(result).hasSize(3);
        assertThat(result.get(Order.Status.NEW)).isEqualTo(1L);
        assertThat(result.get(Order.Status.COMPLETED)).isEqualTo(1L);
        assertThat(result.get(Order.Status.PROCESSING)).isEqualTo(1L);
        verify(orderRepository).findAll();
    }

    /**
     * Test calculateCompletedRevenue demonstrates Streams filter + reduce.
     */
    @Test
    void testCalculateCompletedRevenue() {
        when(orderRepository.findAll())
                .thenReturn(List.of(order1, order2, order3));

        BigDecimal result = orderService.calculateCompletedRevenue();

        // only order2 is COMPLETED: 5 * 25.00 = 125.00
        assertThat(result).isEqualTo(BigDecimal.valueOf(125.00));
        verify(orderRepository).findAll();
    }

    /**
     * Test calculateTotalQuantity demonstrates Streams mapToLong + sum.
     */
    @Test
    void testCalculateTotalQuantity() {
        when(orderRepository.findAll())
                .thenReturn(List.of(order1, order2, order3));

        long result = orderService.calculateTotalQuantity();

        // 2 + 5 + 1 = 8
        assertThat(result).isEqualTo(8L);
        verify(orderRepository).findAll();
    }

    /**
     * Test calculateTotalRevenue demonstrates Streams reduce.
     */
    @Test
    void testCalculateTotalRevenue() {
        when(orderRepository.findAll())
                .thenReturn(List.of(order1, order2, order3));

        BigDecimal result = orderService.calculateTotalRevenue();

        // (2 * 899.99) + (5 * 25.00) + (1 * 120.00) = 1799.98 + 125.00 + 120.00 = 2044.98
        assertThat(result).isEqualTo(BigDecimal.valueOf(2044.98));
        verify(orderRepository).findAll();
    }

    /**
     * Test updateStatus updates and returns order.
     */
    @Test
    void testUpdateStatus() {
        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order1));

        when(orderRepository.save(any(Order.class)))
                .thenReturn(order1);

        Optional<Order> result = orderService.updateStatus(1L, Order.Status.COMPLETED);

        assertThat(result).isPresent();
        verify(orderRepository).findById(1L);
        verify(orderRepository).save(any(Order.class));
    }

    /**
     * Test updateStatus with non-existent id returns empty.
     */
    @Test
    void testUpdateStatus_NotFound() {
        when(orderRepository.findById(999L))
                .thenReturn(Optional.empty());

        Optional<Order> result = orderService.updateStatus(999L, Order.Status.COMPLETED);

        assertThat(result).isEmpty();
        verify(orderRepository).findById(999L);
        verify(orderRepository, never()).save(any());
    }

    /**
     * Test countTotalOrders.
     */
    @Test
    void testCountTotalOrders() {
        when(orderRepository.count())
                .thenReturn(3L);

        long result = orderService.countTotalOrders();

        assertThat(result).isEqualTo(3L);
        verify(orderRepository).count();
    }

    /**
     * Test updateOrder updates mutable fields and returns updated entity when order exists.
     *
     * <p>Verifies trimmed text values and repository save invocation.</p>
     */
    @Test
    void testUpdateOrder() {
        OrderCreateRequest updateRequest = OrderCreateRequest.builder()
                .customerName("  Alice Updated  ")
                .itemName("  Gaming Laptop  ")
                .quantity(3)
                .unitPrice(BigDecimal.valueOf(999.99))
                .build();

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order1));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Order> result = orderService.updateOrder(1L, updateRequest);

        assertThat(result).isPresent();
        assertThat(result.get().getCustomerName()).isEqualTo("Alice Updated");
        assertThat(result.get().getItemName()).isEqualTo("Gaming Laptop");
        assertThat(result.get().getQuantity()).isEqualTo(3);
        assertThat(result.get().getUnitPrice()).isEqualTo(BigDecimal.valueOf(999.99));
        verify(orderRepository).findById(1L);
        verify(orderRepository).save(order1);
    }

    /**
     * Test updateOrder returns empty when target order id does not exist.
     *
     * <p>Ensures save is never called for missing entities.</p>
     */
    @Test
    void testUpdateOrder_NotFound() {
        OrderCreateRequest updateRequest = OrderCreateRequest.builder()
                .customerName("Updated")
                .itemName("Updated Item")
                .quantity(1)
                .unitPrice(BigDecimal.valueOf(10.00))
                .build();

        when(orderRepository.findById(999L))
                .thenReturn(Optional.empty());

        Optional<Order> result = orderService.updateOrder(999L, updateRequest);

        assertThat(result).isEmpty();
        verify(orderRepository).findById(999L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    /**
     * Test deleteOrder returns true and deletes entity when order exists.
     */
    @Test
    void testDeleteOrder() {
        when(orderRepository.existsById(1L))
                .thenReturn(true);

        boolean result = orderService.deleteOrder(1L);

        assertThat(result).isTrue();
        verify(orderRepository).existsById(1L);
        verify(orderRepository).deleteById(1L);
    }

    /**
     * Test deleteOrder returns false and skips deletion when order does not exist.
     */
    @Test
    void testDeleteOrder_NotFound() {
        when(orderRepository.existsById(999L))
                .thenReturn(false);

        boolean result = orderService.deleteOrder(999L);

        assertThat(result).isFalse();
        verify(orderRepository).existsById(999L);
        verify(orderRepository, never()).deleteById(anyLong());
    }
}