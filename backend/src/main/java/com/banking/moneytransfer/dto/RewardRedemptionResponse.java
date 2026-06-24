package com.banking.moneytransfer.dto;

import java.time.LocalDateTime;

public record RewardRedemptionResponse(
    Long id,
    int pointsRedeemed,
    String rewardItem,
    LocalDateTime createdAt
) {}
