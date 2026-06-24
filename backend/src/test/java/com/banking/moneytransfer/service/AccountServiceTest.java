package com.banking.moneytransfer.service;

import com.banking.moneytransfer.domain.Account;
import com.banking.moneytransfer.domain.AccountStatus;
import com.banking.moneytransfer.domain.AccountType;
import com.banking.moneytransfer.domain.User;
import com.banking.moneytransfer.exception.AccountNotFoundException;
import com.banking.moneytransfer.repository.AccountRepository;
import com.banking.moneytransfer.repository.TransactionLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionLogRepository transactionLogRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    public void testGetAccount_Success() {
        // Arrange
        String accNum = "APXAC12345";
        Account account = Account.builder()
                .accountNumber(accNum)
                .balance(BigDecimal.TEN)
                .status(AccountStatus.ACTIVE)
                .build();
        when(accountRepository.findByAccountNumber(accNum)).thenReturn(Optional.of(account));

        // Act
        Account result = accountService.getAccount(accNum);

        // Assert
        assertNotNull(result);
        assertEquals(accNum, result.getAccountNumber());
        verify(accountRepository, times(1)).findByAccountNumber(accNum);
    }

    @Test
    public void testGetAccount_NotFound() {
        // Arrange
        String accNum = "INVALID";
        when(accountRepository.findByAccountNumber(accNum)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AccountNotFoundException.class, () -> accountService.getAccount(accNum));
        verify(accountRepository, times(1)).findByAccountNumber(accNum);
    }

    @Test
    public void testGetBalance_Success() {
        // Arrange
        String accNum = "APXAC12345";
        Account account = Account.builder()
                .accountNumber(accNum)
                .balance(BigDecimal.valueOf(100.50))
                .status(AccountStatus.ACTIVE)
                .build();
        when(accountRepository.findByAccountNumber(accNum)).thenReturn(Optional.of(account));

        // Act
        BigDecimal balance = accountService.getBalance(accNum);

        // Assert
        assertEquals(BigDecimal.valueOf(100.50), balance);
    }
}
