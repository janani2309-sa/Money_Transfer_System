package com.banking.moneytransfer.controller;

import com.banking.moneytransfer.domain.Account;
import com.banking.moneytransfer.domain.User;
import com.banking.moneytransfer.dto.TransferRequest;
import com.banking.moneytransfer.repository.AccountRepository;
import com.banking.moneytransfer.repository.RewardRepository;
import com.banking.moneytransfer.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class RewardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RewardRepository rewardRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @BeforeEach
    public void setUp() {
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE rewards").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE transaction_logs").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE accounts").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE users").executeUpdate();
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();

        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

        User janani = User.builder()
                .username("janani")
                .password(encoder.encode("Janani23092004"))
                .firstName("John")
                .lastName("Smith")
                .email("janani@apexpay.com")
                .phoneNumber("1234567890")
                .isVerified(true)
                .build();
        userRepository.save(janani);

        User jane = User.builder()
                .username("jane")
                .password(encoder.encode("password123"))
                .firstName("Jane")
                .lastName("Doe")
                .email("jane@apexpay.com")
                .phoneNumber("5551234567")
                .isVerified(true)
                .build();
        userRepository.save(jane);

        Account acc1 = Account.builder()
                .id(1L)
                .accountNumber("APXAC00001")
                .user(janani)
                .accountType(com.banking.moneytransfer.domain.AccountType.SAVINGS)
                .firstName("John")
                .lastName("Smith")
                .balance(new BigDecimal("45250.00"))
                .status(com.banking.moneytransfer.domain.AccountStatus.ACTIVE)
                .version(0)
                .build();
        accountRepository.save(acc1);

        Account acc2 = Account.builder()
                .id(2L)
                .accountNumber("APXAC00002")
                .user(jane)
                .accountType(com.banking.moneytransfer.domain.AccountType.SAVINGS)
                .firstName("Jane")
                .lastName("Doe")
                .balance(new BigDecimal("10000.00"))
                .status(com.banking.moneytransfer.domain.AccountStatus.ACTIVE)
                .version(0)
                .build();
        accountRepository.save(acc2);
    }

    @Test
    public void testGetRewards_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/rewards/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetRewards_NoRewards() throws Exception {
        mockMvc.perform(get("/api/v1/rewards/me")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("janani", "Janani23092004")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPoints").value(0))
                .andExpect(jsonPath("$.history").isEmpty());
    }

    @Test
    public void testTransfer_EarnsReward() throws Exception {
        TransferRequest request = new TransferRequest(
                "APXAC00001", // John Smith
                "APXAC00002", // Jane Doe
                new BigDecimal("250.00"), // 2 points expected
                UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/api/v1/transfers")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("janani", "Janani23092004"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/rewards/me")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("janani", "Janani23092004")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPoints").value(2))
                .andExpect(jsonPath("$.history.length()").value(1))
                .andExpect(jsonPath("$.history[0].fromAccountNumber").value("APXAC00001"))
                .andExpect(jsonPath("$.history[0].toAccountNumber").value("APXAC00002"))
                .andExpect(jsonPath("$.history[0].transactionAmount").value(250.00))
                .andExpect(jsonPath("$.history[0].pointsEarned").value(2));
    }

    @Test
    public void testTransfer_SelfTransfer_NoReward() throws Exception {
        // Create second account for janani
        User janani = userRepository.findByUsername("janani").orElseThrow();
        Account acc3 = Account.builder()
                .id(3L)
                .accountNumber("APXAC00003")
                .user(janani)
                .accountType(com.banking.moneytransfer.domain.AccountType.BUSINESS)
                .firstName("John")
                .lastName("Smith")
                .balance(new BigDecimal("5000.00"))
                .status(com.banking.moneytransfer.domain.AccountStatus.ACTIVE)
                .version(0)
                .build();
        accountRepository.save(acc3);

        TransferRequest request = new TransferRequest(
                "APXAC00001",
                "APXAC00003", // Owned by same user
                new BigDecimal("250.00"),
                UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/api/v1/transfers")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("janani", "Janani23092004"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/rewards/me")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("janani", "Janani23092004")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPoints").value(0))
                .andExpect(jsonPath("$.history").isEmpty());
    }

    @Test
    public void testTransfer_BelowLimit_NoReward() throws Exception {
        TransferRequest request = new TransferRequest(
                "APXAC00001",
                "APXAC00002",
                new BigDecimal("99.00"), // Below 100 limit
                UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/api/v1/transfers")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("janani", "Janani23092004"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/rewards/me")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("janani", "Janani23092004")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPoints").value(0))
                .andExpect(jsonPath("$.history").isEmpty());
    }

    @Test
    public void testRedeemRewards_Success_500() throws Exception {
        User janani = userRepository.findByUsername("janani").orElseThrow();
        com.banking.moneytransfer.domain.Reward reward = com.banking.moneytransfer.domain.Reward.builder()
                .user(janani)
                .pointsEarned(600)
                .transactionLog(com.banking.moneytransfer.domain.TransactionLog.builder()
                        .fromAccountId(1L)
                        .toAccountId(2L)
                        .amount(new BigDecimal("60000.00"))
                        .status(com.banking.moneytransfer.domain.TransactionStatus.SUCCESS)
                        .idempotencyKey(UUID.randomUUID().toString())
                        .build())
                .build();
        entityManager.persist(reward.getTransactionLog());
        rewardRepository.save(reward);
        entityManager.flush();

        com.banking.moneytransfer.dto.RedeemRewardRequest request = 
                new com.banking.moneytransfer.dto.RedeemRewardRequest(500);

        mockMvc.perform(post("/api/v1/rewards/redeem")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("janani", "Janani23092004"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.pointsRedeemed").value(500))
                .andExpect(jsonPath("$.remainingBalance").value(100))
                .andExpect(jsonPath("$.rewardItem").exists());

        mockMvc.perform(get("/api/v1/rewards/me")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("janani", "Janani23092004")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPoints").value(100))
                .andExpect(jsonPath("$.totalEarned").value(600))
                .andExpect(jsonPath("$.totalRedeemed").value(500))
                .andExpect(jsonPath("$.redemptions.length()").value(1))
                .andExpect(jsonPath("$.redemptions[0].pointsRedeemed").value(500));
    }

    @Test
    public void testRedeemRewards_InsufficientPoints() throws Exception {
        com.banking.moneytransfer.dto.RedeemRewardRequest request = 
                new com.banking.moneytransfer.dto.RedeemRewardRequest(500);

        mockMvc.perform(post("/api/v1/rewards/redeem")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("janani", "Janani23092004"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void testRedeemRewards_InvalidAmount() throws Exception {
        com.banking.moneytransfer.dto.RedeemRewardRequest request = 
                new com.banking.moneytransfer.dto.RedeemRewardRequest(100);

        mockMvc.perform(post("/api/v1/rewards/redeem")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("janani", "Janani23092004"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void testRedeemRewards_Unauthorized() throws Exception {
        com.banking.moneytransfer.dto.RedeemRewardRequest request = 
                new com.banking.moneytransfer.dto.RedeemRewardRequest(500);

        mockMvc.perform(post("/api/v1/rewards/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
