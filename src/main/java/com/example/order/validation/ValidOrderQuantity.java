package com.example.order.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validator for order quantity.
 *
 * <p>Ensures quantity is a positive integer between 1 and 10,000.</p>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = OrderQuantityValidator.class)
@Documented
public @interface ValidOrderQuantity {

    /**
     * Default validation failure message.
     */
    String message() default "Quantity must be between 1 and 10,000 units";

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
 * Validator implementation for {@link ValidOrderQuantity}.
 */
class OrderQuantityValidator implements ConstraintValidator<ValidOrderQuantity, Integer> {

    private static final int MIN_QTY = 1;
    private static final int MAX_QTY = 10_000;

    @Override
    public void initialize(ValidOrderQuantity constraintAnnotation) {
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }

        boolean valid = value >= MIN_QTY && value <= MAX_QTY;

        if (!valid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    String.format("Quantity must be between %d and %d (received: %d)", MIN_QTY, MAX_QTY, value)
            ).addConstraintViolation();
        }

        return valid;
    }
}