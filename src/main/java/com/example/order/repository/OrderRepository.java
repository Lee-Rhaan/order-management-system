package com.example.order.repository;

import com.example.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Order entity persistence.
 *
 * <p>Provides CRUD operations and custom query derivations for common search scenarios.</p>
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Finds orders by customer name (case-insensitive substring).
     *
     * @param customerName search keyword
     * @return matching orders
     */
    List<Order> findByCustomerNameContainingIgnoreCase(String customerName);

    /**
     * Finds orders by exact status.
     *
     * @param status target status
     * @return orders with that status
     */
    List<Order> findByStatus(Order.Status status);

    /**
     * Finds orders by item name (case-insensitive substring).
     *
     * @param itemName search keyword
     * @return matching orders
     */
    List<Order> findByItemNameContainingIgnoreCase(String itemName);
}