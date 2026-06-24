package com.banking.moneytransfer.controller;

import com.banking.moneytransfer.domain.Account;
import com.banking.moneytransfer.domain.AccountStatus;
import com.banking.moneytransfer.domain.AccountType;
import com.banking.moneytransfer.domain.User;
import com.banking.moneytransfer.dto.CreateAccountRequest;
import com.banking.moneytransfer.repository.AccountRepository;
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
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AccountControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @BeforeEach
    public void setUp() {
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE transaction_logs").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE accounts").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE users").executeUpdate();
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();

        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

        User user = User.builder()
                .username("testuser")
                .password(encoder.encode("Password123!"))
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .phoneNumber("1234567890")
                .isVerified(true)
                .build();
        userRepository.save(user);

        Account account = Account.builder()
                .accountNumber("APXAC12345")
                .user(user)
                .accountType(AccountType.SAVINGS)
                .firstName("John")
                .lastName("Doe")
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE)
                .version(0)
                .build();
        accountRepository.save(account);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    public void testGetBalance_Success() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/APXAC12345/balance")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("testuser", "Password123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("APXAC12345"))
                .andExpect(jsonPath("$.balance").value(1000.00));
    }

    @Test
    public void testGetTransactions_Success() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/APXAC12345/transactions")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("testuser", "Password123!")))
                .andExpect(status().isOk());
    }

    @Test
    public void testVerifyAccount_Success() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/APXAC12345/verify")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("testuser", "Password123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("APXAC12345"))
                .andExpect(jsonPath("$.accountHolderName").value("John Doe"))
                .andExpect(jsonPath("$.isValid").value(true));
    }

    @Test
    public void testDownloadStatement_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/APXAC12345/statement/pdf"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testOpenAccount_Success() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest(
                AccountType.BUSINESS, // Different type than existing SAVINGS account
                new BigDecimal("2000.00"),
                "AADHAAR_CARD",
                "1234567890"
        );

        mockMvc.perform(post("/api/v1/accounts")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("testuser", "Password123!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountType").value("BUSINESS"))
                .andExpect(jsonPath("$.balance").value(2000.00));
    }

    @Test
    public void testOpenAccount_DuplicateTypeThrowsException() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest(
                AccountType.SAVINGS, // Duplicate type
                new BigDecimal("2000.00"),
                "AADHAAR_CARD",
                "1234567890"
        );

        mockMvc.perform(post("/api/v1/accounts")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("testuser", "Password123!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void testOpenAccount_Unauthorized() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest(
                AccountType.SAVINGS,
                new BigDecimal("2000.00"),
                "AADHAAR_CARD",
                "1234567890"
        );

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testCloseAccount_Success() throws Exception {
        // Change balance to 0 first to satisfy business rule: "Cannot close account. Account balance must be exactly Rs. 0.00."
        Account acc = accountRepository.findByAccountNumber("APXAC12345").orElseThrow();
        acc.setBalance(BigDecimal.ZERO);
        accountRepository.saveAndFlush(acc);

        Map<String, String> body = new HashMap<>();
        body.put("reason", "No longer needed");

        mockMvc.perform(patch("/api/v1/accounts/APXAC12345/close")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("testuser", "Password123!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.closureReason").value("No longer needed"));
    }

    @Test
    public void testCloseAccount_NonZeroBalanceThrowsException() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("reason", "No longer needed");

        mockMvc.perform(patch("/api/v1/accounts/APXAC12345/close")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("testuser", "Password123!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void testCloseAccount_Unauthorized() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("reason", "No longer needed");

        mockMvc.perform(patch("/api/v1/accounts/APXAC12345/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }
}
