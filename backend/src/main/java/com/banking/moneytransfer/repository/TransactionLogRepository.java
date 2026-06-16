package com.banking.moneytransfer.repository;

import com.banking.moneytransfer.domain.TransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, String> {
    Optional<TransactionLog> findByIdempotencyKey(String idempotencyKey);
    List<TransactionLog> findByFromAccountIdOrToAccountIdOrderByCreatedOnDesc(Long fromAccountId, Long toAccountId);
}
