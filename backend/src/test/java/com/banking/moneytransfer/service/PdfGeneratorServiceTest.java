package com.banking.moneytransfer.service;

import com.banking.moneytransfer.domain.Account;
import com.banking.moneytransfer.domain.AccountStatus;
import com.banking.moneytransfer.domain.AccountType;
import com.banking.moneytransfer.dto.TransactionLogResponse;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PdfGeneratorServiceTest {

    private final PdfGeneratorService pdfGeneratorService = new PdfGeneratorService();

    @Test
    public void testGenerateStatementPdf_NoTransactions() {
        // Arrange
        Account account = Account.builder()
                .accountNumber("APXAC00001")
                .firstName("John")
                .lastName("Smith")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("5000.00"))
                .status(AccountStatus.ACTIVE)
                .build();
        List<TransactionLogResponse> transactions = Collections.emptyList();

        // Act
        ByteArrayInputStream pdfStream = pdfGeneratorService.generateStatementPdf(account, transactions);

        // Assert
        assertNotNull(pdfStream);
        assertTrue(pdfStream.available() > 0);
    }

    @Test
    public void testGenerateStatementPdf_WithTransactions() {
        // Arrange
        Account account = Account.builder()
                .accountNumber("APXAC00001")
                .firstName("John")
                .lastName("Smith")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("5000.00"))
                .status(AccountStatus.ACTIVE)
                .build();

        // Transaction 1: Debit, Success, Long ID (> 8 characters)
        TransactionLogResponse tx1 = new TransactionLogResponse(
                "TX-VERY-LONG-ID-123456789",
                "APXAC00001",
                "APXAC00002",
                new BigDecimal("200.00"),
                "SUCCESS",
                null,
                "idempotency-1",
                LocalDateTime.now()
        );

        // Transaction 2: Credit, Success, Short ID (<= 8 characters)
        TransactionLogResponse tx2 = new TransactionLogResponse(
                "TX2",
                "APXAC00002",
                "APXAC00001",
                new BigDecimal("150.00"),
                "SUCCESS",
                null,
                "idempotency-2",
                LocalDateTime.now()
        );

        // Transaction 3: Debit, Failed, Long ID (> 8 characters)
        TransactionLogResponse tx3 = new TransactionLogResponse(
                "TX-VERY-LONG-ID-FAILED",
                "APXAC00001",
                "APXAC00003",
                new BigDecimal("1000.00"),
                "FAILED",
                "Insufficient funds",
                "idempotency-3",
                LocalDateTime.now()
        );

        List<TransactionLogResponse> transactions = Arrays.asList(tx1, tx2, tx3);

        // Act
        ByteArrayInputStream pdfStream = pdfGeneratorService.generateStatementPdf(account, transactions);

        // Assert
        assertNotNull(pdfStream);
        assertTrue(pdfStream.available() > 0);
    }
}
