package com.example.order.dto;

import com.example.order.validation.ValidBigDecimal;
import com.example.order.validation.ValidOrderQuantity;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating a new order via REST API or form submission.
 *
 * <p>Enforces comprehensive validation at the DTO layer with custom constraints
 * and specific error messages to guide API clients toward correct payloads.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreateRequest {

    /**
     * Customer name placing the order.
     * Must not be blank, max 120 chars, and contain only letters, spaces, and hyphens.
     */
    @NotBlank(message = "Customer name is required")
    @Size(min = 2, max = 120, message = "Customer name must be between 2 and 120 characters")
    @Pattern(
            regexp = "^[a-zA-Z\\s\\-']+$",
            message = "Customer name can only contain letters, spaces, hyphens, and apostrophes"
    )
    private String customerName;

    /**
     * Item/product name being ordered.
     * Must not be blank, max 150 chars.
     */
    @NotBlank(message = "Item name is required")
    @Size(min = 2, max = 150, message = "Item name must be between 2 and 150 characters")
    private String itemName;

    /**
     * Quantity of items ordered.
     * Custom validator ensures it's between 1 and 10000.
     */
    @NotNull(message = "Quantity is required")
    @ValidOrderQuantity
    private Integer quantity;

    /**
     * Unit price for one item.
     * Custom validator ensures valid decimal format and positive value.
     */
    @NotNull(message = "Unit price is required")
    @ValidBigDecimal
    private BigDecimal unitPrice;
}