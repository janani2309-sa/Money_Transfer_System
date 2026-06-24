package com.banking.moneytransfer.service;

import com.banking.moneytransfer.domain.Account;
import com.banking.moneytransfer.domain.TransactionLog;
import com.banking.moneytransfer.domain.TransactionStatus;
import com.banking.moneytransfer.dto.TransferRequest;
import com.banking.moneytransfer.repository.AccountRepository;
import com.banking.moneytransfer.repository.TransactionLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class FailedTransactionLogger {

    private final TransactionLogRepository transactionLogRepository;
    private final AccountRepository accountRepository;

    @Autowired
    public FailedTransactionLogger(TransactionLogRepository transactionLogRepository, AccountRepository accountRepository) {
        this.transactionLogRepository = transactionLogRepository;
        this.accountRepository = accountRepository;
    }


    @Async
    @Transactional
    public void logFailedTransaction(TransferRequest request, String reason) {
        if (transactionLogRepository.findByIdempotencyKey(request.idempotencyKey()).isEmpty()) {
            Long fromId = -1L;
            Long toId = -1L;
            
            try {
                if (request.fromAccountNumber() != null) {
                    fromId = accountRepository.findByAccountNumber(request.fromAccountNumber())
                            .map(Account::getId).orElse(-1L);
                }
            } catch (Exception e) {}
            
            try {
                if (request.toAccountNumber() != null) {
                    toId = accountRepository.findByAccountNumber(request.toAccountNumber())
                            .map(Account::getId).orElse(-1L);
                }
            } catch (Exception e) {}

            TransactionLog log = TransactionLog.builder()
                    .fromAccountId(fromId)
                    .toAccountId(toId)
                    .amount(request.amount() != null ? request.amount() : BigDecimal.ZERO)
                    .status(TransactionStatus.FAILED)
                    .failureReason(reason)
                    .idempotencyKey(request.idempotencyKey())
                    .build();
            transactionLogRepository.save(log);
        }
    }
}
