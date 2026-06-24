package com.banking.moneytransfer.repository;

import com.banking.moneytransfer.domain.RewardRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RewardRedemptionRepository extends JpaRepository<RewardRedemption, Long> {
    List<RewardRedemption> findByUserIdOrderByCreatedAtDesc(Long userId);
}
