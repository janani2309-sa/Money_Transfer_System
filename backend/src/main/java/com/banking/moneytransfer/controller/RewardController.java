package com.banking.moneytransfer.controller;

import com.banking.moneytransfer.domain.User;
import com.banking.moneytransfer.dto.RewardSummaryResponse;
import com.banking.moneytransfer.repository.UserRepository;
import com.banking.moneytransfer.service.RewardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/api/v1/rewards")
@CrossOrigin(origins = "*")
public class RewardController {

    private final RewardService rewardService;
    private final UserRepository userRepository;

    @Autowired
    public RewardController(RewardService rewardService, UserRepository userRepository) {
        this.rewardService = rewardService;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<RewardSummaryResponse> getMyRewards(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + principal.getName()));

        RewardSummaryResponse summary = rewardService.getRewardSummaryForUser(user);
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/redeem")
    public ResponseEntity<com.banking.moneytransfer.dto.RedeemRewardResponse> redeemRewards(
            Principal principal,
            @RequestBody com.banking.moneytransfer.dto.RedeemRewardRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + principal.getName()));

        com.banking.moneytransfer.dto.RedeemRewardResponse response = rewardService.redeemRewardPoints(user, request.pointsToRedeem());
        return ResponseEntity.ok(response);
    }
}
