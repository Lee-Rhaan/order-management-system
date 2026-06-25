package com.example.order.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.math.BigDecimal;

/**
 * Custom validator for BigDecimal price fields.
 *
 * <p>Ensures the price is positive, has at most 2 decimal places,
 * and does not exceed $999,999.99.</p>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BigDecimalValidator.class)
@Documented
public @interface ValidBigDecimal {

    /**
     * Default validation failure message.
     */
    String message() default "Price must be between 0.01 and 999,999.99 with at most 2 decimal places";

    /**
     * Validation groups.
     */
    Class<?>[] groups() default {};

    /**
     * Additional payload for clients to assign custom error handling.
     */
    Class<? extends Payload>[] payload() default {};
}

/**
 * Validator implementation for {@link ValidBigDecimal}.
 */
class BigDecimalValidator implements ConstraintValidator<ValidBigDecimal, BigDecimal> {

    private static final BigDecimal MIN_PRICE = new BigDecimal("0.01");
    private static final BigDecimal MAX_PRICE = new BigDecimal("999999.99");

    @Override
    public void initialize(ValidBigDecimal constraintAnnotation) {
    }

    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }

        // Check if value is positive and within range
        boolean isPriceInRange = value.compareTo(MIN_PRICE) >= 0 && value.compareTo(MAX_PRICE) <= 0;

        // Check decimal places (max 2)
        int decimalPlaces = value.scale() < 0 ? 0 : value.scale();
        boolean hasValidDecimals = decimalPlaces <= 2;

        boolean valid = isPriceInRange && hasValidDecimals;

        if (!valid) {
            context.disableDefaultConstraintViolation();
            String message;
            if (!isPriceInRange) {
                message = String.format(
                        "Price must be between $%.2f and $%.2f (received: $%.2f)",
                        MIN_PRICE, MAX_PRICE, value
                );
            } else {
                message = String.format(
                        "Price must have at most 2 decimal places (received: %d places)",
                        decimalPlaces
                );
            }
            context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        }

        return valid;
    }
}