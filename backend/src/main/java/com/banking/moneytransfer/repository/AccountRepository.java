package com.banking.moneytransfer.repository;

import com.banking.moneytransfer.domain.Account;
import com.banking.moneytransfer.domain.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    Optional<Account> findByAccountNumber(String accountNumber);
    List<Account> findByUserId(Long userId);
    Optional<Account> findByUserIdAndAccountType(Long userId, AccountType accountType);
}
