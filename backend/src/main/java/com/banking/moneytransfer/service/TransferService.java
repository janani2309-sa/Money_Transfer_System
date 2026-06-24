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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final RewardService rewardService;
    private final FailedTransactionLogger failedTransactionLogger;

    @Autowired
    public TransferService(AccountRepository accountRepository, 
                           TransactionLogRepository transactionLogRepository, 
                           RewardService rewardService,
                           FailedTransactionLogger failedTransactionLogger) {
        this.accountRepository = accountRepository;
        this.transactionLogRepository = transactionLogRepository;
        this.rewardService = rewardService;
        this.failedTransactionLogger = failedTransactionLogger;
    }

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        Optional<TransactionLog> existingTx = transactionLogRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existingTx.isPresent()) {
            throw new DuplicateTransferException("Transaction with idempotency key '" + request.idempotencyKey() + "' was already submitted.");
        }

        if (request.fromAccountNumber().equals(request.toAccountNumber())) {
            failedTransactionLogger.logFailedTransaction(request, "Source and destination accounts must be different");
            throw new IllegalArgumentException("Source and destination accounts must be different");
        }

        Account sourceAccount = accountRepository.findByAccountNumber(request.fromAccountNumber())
                .orElseThrow(() -> {
                    failedTransactionLogger.logFailedTransaction(request, "Source account not found");
                    return new AccountNotFoundException("Source account " + request.fromAccountNumber() + " not found");
                });

        Account destinationAccount = accountRepository.findByAccountNumber(request.toAccountNumber())
                .orElseThrow(() -> {
                    failedTransactionLogger.logFailedTransaction(request, "Destination account not found");
                    return new AccountNotFoundException("Destination account " + request.toAccountNumber() + " not found");
                });

        try {
            validateTransfer(sourceAccount, destinationAccount, request.amount());

            executeTransfer(sourceAccount, destinationAccount, request.amount());

            accountRepository.save(sourceAccount);
            accountRepository.save(destinationAccount);

            TransactionLog log = TransactionLog.builder()
                    .fromAccountId(sourceAccount.getId())
                    .toAccountId(destinationAccount.getId())
                    .amount(request.amount())
                    .status(TransactionStatus.SUCCESS)
                    .idempotencyKey(request.idempotencyKey())
                    .build();
            transactionLogRepository.save(log);

            rewardService.processTransactionForRewards(log, sourceAccount, destinationAccount);

            return new TransferResponse(
                    log.getId(),
                    "SUCCESS",
                    "Transfer completed successfully",
                    sourceAccount.getAccountNumber(),
                    destinationAccount.getAccountNumber(),
                    request.amount()
            );

        } catch (BankingException ex) {
            failedTransactionLogger.logFailedTransaction(request, ex.getMessage());
            throw ex;
        }
    }

    public void validateTransfer(Account source, Account target, BigDecimal amount) {
        if (!source.isActive()) {
            throw new AccountNotActiveException("Source account " + source.getAccountNumber() + " is " + source.getStatus());
        }

        if (!target.isActive()) {
            throw new AccountNotActiveException("Destination account " + target.getAccountNumber() + " is " + target.getStatus());
        }

        BigDecimal remaining = source.getBalance().subtract(amount);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientBalanceException("Insufficient funds. Transaction declined.");
        }
        if (remaining.compareTo(BigDecimal.ZERO) > 0 && remaining.compareTo(new BigDecimal("1000.00")) < 0) {
            throw new InsufficientBalanceException("Insufficient funds. Transaction declined. Account balance cannot fall below the minimum balance of 1000.00 Rs.");
        }

        BigDecimal newTargetBalance = target.getBalance().add(amount);
        if (newTargetBalance.compareTo(new BigDecimal("10000000.00")) > 0) {
            throw new IllegalArgumentException("Transaction declined. Recipient account balance would exceed the maximum limit of 10,000,000.00 Rs.");
        }
    }

    public void executeTransfer(Account source, Account target, BigDecimal amount) {
        source.debit(amount);
        target.credit(amount);
    }
}

