package com.banking.moneytransfer.domain;

import com.banking.moneytransfer.exception.InsufficientBalanceException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class AccountTest {

    @Test
    public void testDebit_Success() {
        // Arrange
        Account account = Account.builder()
                .id(1L)
                .accountNumber("APXAC00001")
                .firstName("John")
                .lastName("Doe")
                .balance(new BigDecimal("2000.00"))
                .status(AccountStatus.ACTIVE)
                .build();

        // Act
        account.debit(new BigDecimal("200.00"));

        // Assert
        assertEquals(new BigDecimal("1800.00"), account.getBalance());
    }

    @Test
    public void testDebit_InsufficientBalance() {
        // Arrange
        Account account = Account.builder()
                .id(1L)
                .accountNumber("APXAC00001")
                .firstName("John")
                .lastName("Doe")
                .balance(new BigDecimal("1200.00"))
                .status(AccountStatus.ACTIVE)
                .build();

        // Act & Assert
        assertThrows(InsufficientBalanceException.class, () -> {
            account.debit(new BigDecimal("300.00"));
        });
        // Check that balance did not change
        assertEquals(new BigDecimal("1200.00"), account.getBalance());
    }

    @Test
    public void testCredit_Success() {
        // Arrange
        Account account = Account.builder()
                .id(1L)
                .accountNumber("APXAC00001")
                .firstName("John")
                .lastName("Doe")
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE)
                .build();

        // Act
        account.credit(new BigDecimal("150.00"));

        // Assert
        assertEquals(new BigDecimal("1150.00"), account.getBalance());
    }

    @Test
    public void testCredit_ExceedsMaxBalance() {
        // Arrange
        Account account = Account.builder()
                .id(1L)
                .accountNumber("APXAC00001")
                .firstName("John")
                .lastName("Doe")
                .balance(new BigDecimal("9999900.00"))
                .status(AccountStatus.ACTIVE)
                .build();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            account.credit(new BigDecimal("200.00"));
        });
        assertEquals(new BigDecimal("9999900.00"), account.getBalance());
    }

    @Test
    public void testDebit_RemainingBalanceExactlyZero_Success() {
        // Arrange
        Account account = Account.builder()
                .id(1L)
                .accountNumber("APXAC00001")
                .firstName("John")
                .lastName("Doe")
                .balance(new BigDecimal("1200.00"))
                .status(AccountStatus.ACTIVE)
                .build();

        // Act
        account.debit(new BigDecimal("1200.00"));

        // Assert
        assertEquals(new BigDecimal("0.00"), account.getBalance());
    }

    @Test
    public void testDebit_RemainingBalanceLessThanZero_ThrowsInsufficientBalance() {
        // Arrange
        Account account = Account.builder()
                .id(1L)
                .accountNumber("APXAC00001")
                .firstName("John")
                .lastName("Doe")
                .balance(new BigDecimal("1200.00"))
                .status(AccountStatus.ACTIVE)
                .build();

        // Act & Assert
        assertThrows(InsufficientBalanceException.class, () -> {
            account.debit(new BigDecimal("1300.00"));
        });
        assertEquals(new BigDecimal("1200.00"), account.getBalance());
    }
}
