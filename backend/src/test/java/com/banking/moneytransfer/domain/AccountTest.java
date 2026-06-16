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
                .holderName("John Doe")
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE)
                .build();

        // Act
        account.debit(new BigDecimal("200.00"));

        // Assert
        assertEquals(new BigDecimal("800.00"), account.getBalance());
    }

    @Test
    public void testDebit_InsufficientBalance() {
        // Arrange
        Account account = Account.builder()
                .id(1L)
                .holderName("John Doe")
                .balance(new BigDecimal("100.00"))
                .status(AccountStatus.ACTIVE)
                .build();

        // Act & Assert
        assertThrows(InsufficientBalanceException.class, () -> {
            account.debit(new BigDecimal("200.00"));
        });
        // Check that balance did not change
        assertEquals(new BigDecimal("100.00"), account.getBalance());
    }

    @Test
    public void testCredit_Success() {
        // Arrange
        Account account = Account.builder()
                .id(1L)
                .holderName("John Doe")
                .balance(new BigDecimal("500.00"))
                .status(AccountStatus.ACTIVE)
                .build();

        // Act
        account.credit(new BigDecimal("150.00"));

        // Assert
        assertEquals(new BigDecimal("650.00"), account.getBalance());
    }
}
