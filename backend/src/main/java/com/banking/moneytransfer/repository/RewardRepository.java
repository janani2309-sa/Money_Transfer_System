package com.banking.moneytransfer.repository;

import com.banking.moneytransfer.domain.Reward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RewardRepository extends JpaRepository<Reward, Long> {
    @org.springframework.data.jpa.repository.Query("SELECT r FROM Reward r JOIN FETCH r.transactionLog WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
    List<Reward> findByUserIdOrderByCreatedAtDesc(@org.springframework.data.repository.query.Param("userId") Long userId);
}
