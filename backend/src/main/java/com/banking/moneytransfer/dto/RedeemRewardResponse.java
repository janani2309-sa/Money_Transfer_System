package com.banking.moneytransfer.dto;

public record RedeemRewardResponse(
    boolean success,
    String rewardItem,
    int pointsRedeemed,
    int remainingBalance
) {}
