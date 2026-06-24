package com.banking.moneytransfer.service;

import com.banking.moneytransfer.domain.Account;
import com.banking.moneytransfer.domain.TransactionLog;
import com.banking.moneytransfer.exception.AccountNotFoundException;
import com.banking.moneytransfer.repository.AccountRepository;
import com.banking.moneytransfer.repository.TransactionLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banking.moneytransfer.dto.TransactionLogResponse;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionLogRepository transactionLogRepository;

    @Autowired
    public AccountService(AccountRepository accountRepository, TransactionLogRepository transactionLogRepository) {
        this.accountRepository = accountRepository;
        this.transactionLogRepository = transactionLogRepository;
    }

    public Account getAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account with number " + accountNumber + " not found"));
    }

    public BigDecimal getBalance(String accountNumber) {
        return getAccount(accountNumber).getBalance();
    }

    public List<TransactionLogResponse> getTransactions(String accountNumber) {
        Account account = getAccount(accountNumber);
        List<TransactionLog> logs = transactionLogRepository.findByFromAccountIdOrToAccountIdOrderByCreatedOnDesc(account.getId(), account.getId());
        
        java.util.Set<Long> accountIds = logs.stream()
                .flatMap(log -> java.util.stream.Stream.of(log.getFromAccountId(), log.getToAccountId()))
                .filter(id -> id != null && id > 0)
                .collect(java.util.stream.Collectors.toSet());

        java.util.Map<Long, String> accountMap = new java.util.HashMap<>();
        if (!accountIds.isEmpty()) {
            accountRepository.findAllById(accountIds).forEach(acc -> {
                accountMap.put(acc.getId(), acc.getAccountNumber());
            });
        }

        return logs.stream().map(log -> {
            String fromAccNum = accountMap.getOrDefault(log.getFromAccountId(), "UNKNOWN");
            String toAccNum = accountMap.getOrDefault(log.getToAccountId(), "UNKNOWN");
            return new TransactionLogResponse(
                log.getId(),
                fromAccNum,
                toAccNum,
                log.getAmount(),
                log.getStatus().name(),
                log.getFailureReason(),
                log.getIdempotencyKey(),
                log.getCreatedOn()
            );
        }).toList();
    }

    @Transactional
    public Account openAccount(com.banking.moneytransfer.domain.User user, com.banking.moneytransfer.domain.AccountType type, BigDecimal initialDeposit) {
        List<Account> existingAccounts = accountRepository.findByUserId(user.getId());
        
        java.util.Optional<Account> existingOfSameType = existingAccounts.stream()
                .filter(acc -> acc.getAccountType() == type)
                .findFirst();
                
        if (existingOfSameType.isPresent()) {
            Account account = existingOfSameType.get();
            if (account.getStatus() == com.banking.moneytransfer.domain.AccountStatus.ACTIVE) {
                throw new IllegalArgumentException("User already has an active " + type + " account");
            } else if (account.getStatus() == com.banking.moneytransfer.domain.AccountStatus.CLOSED) {
                if (account.getLastUpdated().plusDays(15).isAfter(java.time.LocalDateTime.now())) {
                    throw new IllegalArgumentException("Cannot reopen account within 15 days of closure of account");
                }
                
                if (initialDeposit == null || initialDeposit.compareTo(new BigDecimal("1000.00")) < 0) {
                    throw new IllegalArgumentException("Initial deposit must be at least 1000.00 Rs");
                }
                if (initialDeposit.compareTo(new BigDecimal("10000000.00")) > 0) {
                    throw new IllegalArgumentException("Initial deposit cannot exceed 10,000,000.00 Rs");
                }
                
                account.setStatus(com.banking.moneytransfer.domain.AccountStatus.ACTIVE);
                account.setBalance(initialDeposit);
                account.setClosureReason(null);
                return accountRepository.save(account);
            }
        }

        if (existingAccounts.size() >= 3) {
            throw new IllegalArgumentException("User cannot have more than 3 banking accounts in total");
        }

        if (initialDeposit == null || initialDeposit.compareTo(new BigDecimal("1000.00")) < 0) {
            throw new IllegalArgumentException("Initial deposit must be at least 1000.00 Rs");
        }
        if (initialDeposit.compareTo(new BigDecimal("10000000.00")) > 0) {
            throw new IllegalArgumentException("Initial deposit cannot exceed 10,000,000.00 Rs");
        }

        String accountNumber = generateUniqueAccountNumber();

        Account newAccount = Account.builder()
                .accountNumber(accountNumber)
                .user(user)
                .accountType(type)
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .balance(initialDeposit)
                .status(com.banking.moneytransfer.domain.AccountStatus.ACTIVE)
                .version(0)
                .build();

        return accountRepository.save(newAccount);
    }

    @Transactional
    public Account closeAccount(String accountNumber, String reason, com.banking.moneytransfer.domain.User currentUser) {
        Account account = getAccount(accountNumber);
        
        if (!account.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Unauthorized. You do not own this account.");
        }
        
        if (account.getStatus() != com.banking.moneytransfer.domain.AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Account is already " + account.getStatus());
        }
        
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("Cannot close account. Account balance must be exactly Rs. 0.00. Please transfer out your remaining balance first.");
        }
        
        account.setStatus(com.banking.moneytransfer.domain.AccountStatus.CLOSED);
        account.setClosureReason(reason == null || reason.trim().isEmpty() ? "No reason specified" : reason.trim());
        
        return accountRepository.save(account);
    }

    private String generateUniqueAccountNumber() {
        String number;
        do {
            number = java.util.UUID.randomUUID().toString();
        } while (accountRepository.findByAccountNumber(number).isPresent());
        return number;
    }
}
