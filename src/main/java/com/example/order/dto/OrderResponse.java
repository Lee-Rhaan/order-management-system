package com.example.order.dto;

import com.example.order.model.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for order payloads returned by the API.
 *
 * <p>Immutable representation of an order for REST responses.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    /**
     * Order identifier.
     */
    private Long id;

    /**
     * Customer name.
     */
    private String customerName;

    /**
     * Item/product name.
     */
    private String itemName;

    /**
     * Quantity ordered.
     */
    private Integer quantity;

    /**
     * Unit price per item.
     */
    private BigDecimal unitPrice;

    /**
     * Calculated total amount (quantity × unitPrice).
     */
    private BigDecimal totalAmount;

    /**
     * Current order status.
     */
    private Order.Status status;

    /**
     * Order creation timestamp.
     */
    private LocalDateTime createdAt;

    /**
     * Order last update timestamp.
     */
    private LocalDateTime updatedAt;

    /**
     * Converts an Order entity to an OrderResponse DTO.
     *
     * @param order order entity
     * @return response DTO
     */
    public static OrderResponse fromEntity(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .customerName(order.getCustomerName())
                .itemName(order.getItemName())
                .quantity(order.getQuantity())
                .unitPrice(order.getUnitPrice())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}