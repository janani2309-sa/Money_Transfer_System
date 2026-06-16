package com.banking.moneytransfer.service;

import com.banking.moneytransfer.domain.Account;
import com.banking.moneytransfer.domain.TransactionLog;
import com.banking.moneytransfer.exception.AccountNotFoundException;
import com.banking.moneytransfer.repository.AccountRepository;
import com.banking.moneytransfer.repository.TransactionLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Retrieves an account by ID or throws AccountNotFoundException.
     */
    public Account getAccount(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account with ID " + id + " not found"));
    }

    /**
     * Retrieves the current balance of an account.
     */
    public BigDecimal getBalance(Long id) {
        return getAccount(id).getBalance();
    }

    /**
     * Retrieves the list of transaction logs associated with an account.
     */
    public List<TransactionLog> getTransactions(Long id) {
        // First verify that the account exists
        getAccount(id);
        return transactionLogRepository.findByFromAccountIdOrToAccountIdOrderByCreatedOnDesc(id, id);
    }
}
