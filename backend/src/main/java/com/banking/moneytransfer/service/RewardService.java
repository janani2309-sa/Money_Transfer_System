package com.banking.moneytransfer.service;

import com.banking.moneytransfer.domain.Account;
import com.banking.moneytransfer.domain.Reward;
import com.banking.moneytransfer.domain.RewardRedemption;
import com.banking.moneytransfer.domain.TransactionLog;
import com.banking.moneytransfer.domain.User;
import com.banking.moneytransfer.dto.RedeemRewardResponse;
import com.banking.moneytransfer.dto.RewardDetailResponse;
import com.banking.moneytransfer.dto.RewardRedemptionResponse;
import com.banking.moneytransfer.dto.RewardSummaryResponse;
import com.banking.moneytransfer.repository.AccountRepository;
import com.banking.moneytransfer.repository.RewardRedemptionRepository;
import com.banking.moneytransfer.repository.RewardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RewardService {

    private static final String[] POOL_500 = {
        "Rs. 500 Amazon Gift Card",
        "Rs. 500 Starbucks Voucher",
        "Rs. 500 Myntra Gift Card",
        "1 Month Spotify Premium",
        "Rs. 500 Google Play Voucher"
    };

    private static final String[] POOL_1000 = {
        "Rs. 1000 Amazon Gift Card",
        "Rs. 1000 Starbucks Voucher",
        "Rs. 1000 Apple Gift Card",
        "3 Months Spotify Premium",
        "Rs. 1000 Netflix Gift Card"
    };

    private final RewardRepository rewardRepository;
    private final RewardRedemptionRepository rewardRedemptionRepository;
    private final AccountRepository accountRepository;
    private final java.util.Random random = new java.util.Random();

    @Autowired
    public RewardService(RewardRepository rewardRepository,
                         RewardRedemptionRepository rewardRedemptionRepository,
                         AccountRepository accountRepository) {
        this.rewardRepository = rewardRepository;
        this.rewardRedemptionRepository = rewardRedemptionRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public void processTransactionForRewards(TransactionLog transactionLog, Account fromAccount, Account toAccount) {
        if (transactionLog.getStatus() != com.banking.moneytransfer.domain.TransactionStatus.SUCCESS) {
            return;
        }

        BigDecimal amount = transactionLog.getAmount();
        if (amount == null || amount.compareTo(new BigDecimal("100")) <= 0) {
            return;
        }

        User sender = fromAccount.getUser();
        User receiver = toAccount.getUser();
        if (sender == null || receiver == null || sender.getId().equals(receiver.getId())) {
            return;
        }

        int points = amount.divide(new BigDecimal("100"), 0, RoundingMode.DOWN).intValue();
        if (points <= 0) {
            return;
        }

        Reward reward = Reward.builder()
                .user(sender)
                .transactionLog(transactionLog)
                .pointsEarned(points)
                .build();
        rewardRepository.save(reward);
    }

    @Transactional
    public RedeemRewardResponse redeemRewardPoints(User user, int pointsToRedeem) {
        if (pointsToRedeem != 500 && pointsToRedeem != 1000) {
            throw new IllegalArgumentException("Invalid points amount. Can only redeem 500 or 1000 points.");
        }

        int totalEarned = rewardRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .mapToInt(Reward::getPointsEarned)
                .sum();

        int totalRedeemed = rewardRedemptionRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .mapToInt(RewardRedemption::getPointsRedeemed)
                .sum();

        int activeBalance = totalEarned - totalRedeemed;

        if (activeBalance < pointsToRedeem) {
            throw new IllegalArgumentException("Insufficient reward points balance. Required: " + pointsToRedeem + ", Available: " + activeBalance);
        }

        String[] pool = (pointsToRedeem == 500) ? POOL_500 : POOL_1000;
        String rewardItem = pool[random.nextInt(pool.length)];

        RewardRedemption redemption = RewardRedemption.builder()
                .user(user)
                .pointsRedeemed(pointsToRedeem)
                .rewardItem(rewardItem)
                .build();
        rewardRedemptionRepository.save(redemption);

        int remainingBalance = activeBalance - pointsToRedeem;

        return new RedeemRewardResponse(true, rewardItem, pointsToRedeem, remainingBalance);
    }

    public RewardSummaryResponse getRewardSummaryForUser(User user) {
        List<Reward> rewards = rewardRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        List<RewardRedemption> redemptions = rewardRedemptionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        
        int totalEarned = rewards.stream()
                .mapToInt(Reward::getPointsEarned)
                .sum();

        int totalRedeemed = redemptions.stream()
                .mapToInt(RewardRedemption::getPointsRedeemed)
                .sum();

        int activeBalance = totalEarned - totalRedeemed;

        java.util.Set<Long> accountIds = rewards.stream()
                .flatMap(r -> java.util.stream.Stream.of(r.getTransactionLog().getFromAccountId(), r.getTransactionLog().getToAccountId()))
                .filter(id -> id != null && id > 0)
                .collect(java.util.stream.Collectors.toSet());

        java.util.Map<Long, String> accountMap = new java.util.HashMap<>();
        if (!accountIds.isEmpty()) {
            accountRepository.findAllById(accountIds).forEach(acc -> {
                accountMap.put(acc.getId(), acc.getAccountNumber());
            });
        }

        List<RewardDetailResponse> history = rewards.stream().map(reward -> {
            TransactionLog log = reward.getTransactionLog();
            String fromAccNum = accountMap.getOrDefault(log.getFromAccountId(), "UNKNOWN");
            String toAccNum = accountMap.getOrDefault(log.getToAccountId(), "UNKNOWN");
            
            return new RewardDetailResponse(
                reward.getId(),
                log.getId(),
                fromAccNum,
                toAccNum,
                log.getAmount(),
                reward.getPointsEarned(),
                reward.getCreatedAt()
            );
        }).toList();

        List<RewardRedemptionResponse> redemptionResponses = redemptions.stream().map(red -> 
            new RewardRedemptionResponse(
                red.getId(),
                red.getPointsRedeemed(),
                red.getRewardItem(),
                red.getCreatedAt()
            )
        ).toList();

        return new RewardSummaryResponse(activeBalance, totalEarned, totalRedeemed, history, redemptionResponses);
    }
}
