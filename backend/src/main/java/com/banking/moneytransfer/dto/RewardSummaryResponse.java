package com.banking.moneytransfer.dto;

import java.util.List;

public record RewardSummaryResponse(
    int totalPoints,
    int totalEarned,
    int totalRedeemed,
    List<RewardDetailResponse> history,
    List<RewardRedemptionResponse> redemptions
) {}
