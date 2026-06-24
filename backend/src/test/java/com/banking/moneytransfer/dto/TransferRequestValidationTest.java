package com.banking.moneytransfer.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TransferRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    public static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    public void testValidRequest() {
        TransferRequest request = new TransferRequest("APXAC00001", "APXAC00002", new BigDecimal("100.00"), "idemp-key-123");
        Set<ConstraintViolation<TransferRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void testInvalidAmount() {
        TransferRequest request = new TransferRequest("APXAC00001", "APXAC00002", new BigDecimal("-5.00"), "idemp-key-123");
        Set<ConstraintViolation<TransferRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Amount must be at least 0.01")));
    }

    @Test
    public void testNullFields() {
        TransferRequest request = new TransferRequest(null, "APXAC00002", null, " ");
        Set<ConstraintViolation<TransferRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Source account number is required") || v.getMessage().contains("must not be blank")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Amount is required")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Idempotency key is required")));
    }

    @Test
    public void testExceedsMaxAmount() {
        TransferRequest request = new TransferRequest("APXAC00001", "APXAC00002", new BigDecimal("10000000.01"), "idemp-key-123");
        Set<ConstraintViolation<TransferRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Transfer amount cannot exceed 10,000,000.00 Rs")));
    }
}
