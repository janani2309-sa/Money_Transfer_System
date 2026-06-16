package com.banking.moneytransfer.service;

import com.banking.moneytransfer.domain.Account;
import com.banking.moneytransfer.domain.TransactionLog;
import com.banking.moneytransfer.domain.TransactionStatus;
import com.banking.moneytransfer.dto.TransferRequest;
import com.banking.moneytransfer.dto.TransferResponse;
import com.banking.moneytransfer.exception.*;
import com.banking.moneytransfer.repository.AccountRepository;
import com.banking.moneytransfer.repository.TransactionLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionLogRepository transactionLogRepository;

    @Autowired
    public TransferService(AccountRepository accountRepository, TransactionLogRepository transactionLogRepository) {
        this.accountRepository = accountRepository;
        this.transactionLogRepository = transactionLogRepository;
    }

    /**
     * Executes a fund transfer between accounts.
     * Annotated with @Transactional to ensure the debit and credit happen as a single atomic unit.
     */
    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        // Rule 8: Check Idempotency key uniqueness
        Optional<TransactionLog> existingTx = transactionLogRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existingTx.isPresent()) {
            throw new DuplicateTransferException("Transaction with idempotency key '" + request.idempotencyKey() + "' was already submitted.");
        }

        // Rule 1: Accounts must be different
        if (request.fromAccountId().equals(request.toAccountId())) {
            // We log this failure as well
            logFailedTransaction(request, "Source and destination accounts must be different");
            throw new IllegalArgumentException("Source and destination accounts must be different");
        }

        // Rule 2 & 3: Accounts must exist
        Account sourceAccount = accountRepository.findById(request.fromAccountId())
                .orElseThrow(() -> {
                    logFailedTransaction(request, "Source account not found");
                    return new AccountNotFoundException("Source account " + request.fromAccountId() + " not found");
                });

        Account destinationAccount = accountRepository.findById(request.toAccountId())
                .orElseThrow(() -> {
                    logFailedTransaction(request, "Destination account not found");
                    return new AccountNotFoundException("Destination account " + request.toAccountId() + " not found");
                });

        try {
            // Validate statuses and balances
            validateTransfer(sourceAccount, destinationAccount, request.amount());

            // Execute debit and credit (Rule 9: Debit before credit)
            executeTransfer(sourceAccount, destinationAccount, request.amount());

            // Save entities
            accountRepository.save(sourceAccount);
            accountRepository.save(destinationAccount);

            // Log successful transaction (Rule 10: Log every transfer)
            TransactionLog log = TransactionLog.builder()
                    .fromAccountId(sourceAccount.getId())
                    .toAccountId(destinationAccount.getId())
                    .amount(request.amount())
                    .status(TransactionStatus.SUCCESS)
                    .idempotencyKey(request.idempotencyKey())
                    .build();
            transactionLogRepository.save(log);

            return new TransferResponse(
                    log.getId(),
                    "SUCCESS",
                    "Transfer completed successfully",
                    sourceAccount.getId(),
                    destinationAccount.getId(),
                    request.amount()
            );

        } catch (BankingException ex) {
            // Log the custom banking exceptions (like InsufficientBalance) in a separate transaction
            logFailedTransaction(request, ex.getMessage());
            throw ex; // Rethrow to let controller advice handle it
        }
    }

    /**
     * Validates business logic rules.
     */
    public void validateTransfer(Account source, Account target, BigDecimal amount) {
        // Rule 4: Source account must be ACTIVE
        if (!source.isActive()) {
            throw new AccountNotActiveException("Source account " + source.getId() + " is " + source.getStatus());
        }

        // Rule 5: Destination account must be ACTIVE
        if (!target.isActive()) {
            throw new AccountNotActiveException("Destination account " + target.getId() + " is " + target.getStatus());
        }

        // Rule 7: Source balance >= amount
        if (source.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient funds. Available: " + source.getBalance() + ", Requested: " + amount);
        }
    }

    /**
     * Helper to apply debit and credit.
     */
    public void executeTransfer(Account source, Account target, BigDecimal amount) {
        source.debit(amount);
        target.credit(amount);
    }

    /**
     * Logs failed transactions to the database.
     * propagation = Propagation.REQUIRES_NEW ensures this gets committed even if the main transaction rolls back.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailedTransaction(TransferRequest request, String reason) {
        // Check if log already exists for this idempotency key before inserting
        if (transactionLogRepository.findByIdempotencyKey(request.idempotencyKey()).isEmpty()) {
            TransactionLog log = TransactionLog.builder()
                    .fromAccountId(request.fromAccountId() != null ? request.fromAccountId() : -1L)
                    .toAccountId(request.toAccountId() != null ? request.toAccountId() : -1L)
                    .amount(request.amount() != null ? request.amount() : BigDecimal.ZERO)
                    .status(TransactionStatus.FAILED)
                    .failureReason(reason)
                    .idempotencyKey(request.idempotencyKey())
                    .build();
            transactionLogRepository.save(log);
        }
    }
}
