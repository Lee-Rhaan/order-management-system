package com.example.order.service;

import com.example.order.dto.OrderCreateRequest;
import com.example.order.model.Order;
import com.example.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service layer for order operations.
 *
 * <p>Encapsulates business logic for creating, retrieving, and updating orders.
 * Demonstrates Java Streams for aggregation, grouping, sorting, and filtering.
 * All public methods are transactional.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    /**
     * Repository dependency for data access.
     */
    private final OrderRepository orderRepository;

    /**
     * Creates a new order from validated request data.
     *
     * <p>Strips whitespace from string fields and sets initial status to NEW.
     * Logs the creation with full order details.</p>
     *
     * @param request validated create-order request DTO
     * @return persisted order entity with generated ID
     * @throws IllegalArgumentException if request fields are invalid after trimming
     */
    public Order createOrder(OrderCreateRequest request) {
        log.debug("Creating order for customer: {}", request.getCustomerName());

        Order order = Order.builder()
                .customerName(request.getCustomerName().trim())
                .itemName(request.getItemName().trim())
                .quantity(request.getQuantity())
                .unitPrice(request.getUnitPrice())
                .status(Order.Status.NEW)
                .build();

        Order saved = orderRepository.save(order);
        log.info("Order created successfully: id={} customer='{}' item='{}' qty={} unitPrice={} total={}",
                saved.getId(), saved.getCustomerName(), saved.getItemName(),
                saved.getQuantity(), saved.getUnitPrice(), saved.getTotalAmount());

        return saved;
    }

    /**
     * Retrieves all orders sorted by creation timestamp in descending order.
     *
     * <p>Uses Java Streams for in-memory sorting. No pagination applied.</p>
     *
     * @return list of all orders, sorted newest first
     */
    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll().stream()
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .toList();
    }

    /**
     * Retrieves a single order by its ID.
     *
     * @param id order identifier
     * @return Optional wrapping the order if found
     */
    @Transactional(readOnly = true)
    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    /**
     * Searches for orders by customer name (case-insensitive substring match).
     *
     * @param customerName customer name keyword
     * @return list of matching orders
     */
    @Transactional(readOnly = true)
    public List<Order> searchByCustomerName(String customerName) {
        if (customerName == null || customerName.isBlank()) {
            return Collections.emptyList();
        }
        return orderRepository.findByCustomerNameContainingIgnoreCase(customerName.trim());
    }

    /**
     * Counts orders grouped by their status using Java Streams.
     *
     * @return map of Status → count
     */
    @Transactional(readOnly = true)
    public Map<Order.Status, Long> countByStatus() {
        return orderRepository.findAll().stream()
                .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));
    }

    /**
     * Calculates total revenue from completed orders using Java Streams.
     *
     * @return sum of totalAmounts for COMPLETED orders
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateCompletedRevenue() {
        return orderRepository.findAll().stream()
                .filter(order -> order.getStatus() == Order.Status.COMPLETED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates total quantity across all orders using Java Streams.
     *
     * @return sum of quantities
     */
    @Transactional(readOnly = true)
    public long calculateTotalQuantity() {
        return orderRepository.findAll().stream()
                .filter(order -> order.getQuantity() != null)
                .mapToLong(Order::getQuantity)
                .sum();
    }

    /**
     * Calculates total revenue across all orders using Java Streams.
     *
     * @return sum of totalAmounts
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalRevenue() {
        return orderRepository.findAll().stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Updates the status of an existing order.
     *
     * <p>Logs the status transition for audit purposes.</p>
     *
     * @param id order identifier
     * @param newStatus target status
     * @return Optional wrapping the updated order if found
     */
    public Optional<Order> updateStatus(Long id, Order.Status newStatus) {
        return orderRepository.findById(id).map(order -> {
            Order.Status oldStatus = order.getStatus();
            order.setStatus(newStatus);
            Order updated = orderRepository.save(order);
            log.info("Order status updated: id={} {} → {}", id, oldStatus, newStatus);
            return updated;
        });
    }

    /**
     * Counts total number of orders in the database.
     *
     * @return count of all orders
     */
    @Transactional(readOnly = true)
    public long countTotalOrders() {
        return orderRepository.count();
    }
}