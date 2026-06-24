package com.banking.moneytransfer.controller;

import com.banking.moneytransfer.domain.Account;
import com.banking.moneytransfer.domain.User;
import com.banking.moneytransfer.dto.TransferRequest;
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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // Rolls back database changes after each test
public class TransferControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private com.banking.moneytransfer.repository.TransactionLogRepository transactionLogRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @BeforeEach
    public void setUp() {
        // Clear tables and reset auto-increment IDs using native TRUNCATE queries
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
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

        User alice = User.builder()
                .username("alice")
                .password(encoder.encode("password123"))
                .firstName("Alice")
                .lastName("Brown")
                .email("alice@apexpay.com")
                .phoneNumber("5557654321")
                .isVerified(true)
                .build();
        userRepository.save(alice);

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

        Account acc4 = Account.builder()
                .id(4L)
                .accountNumber("APXAC00004")
                .user(alice)
                .accountType(com.banking.moneytransfer.domain.AccountType.SAVINGS)
                .firstName("Alice")
                .lastName("Brown")
                .balance(new BigDecimal("1500.00"))
                .status(com.banking.moneytransfer.domain.AccountStatus.ACTIVE)
                .version(0)
                .build();
        accountRepository.save(acc4);
    }

    @Test
    public void testGetAccount_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/APXAC00001"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetAccount_Success() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/APXAC00001")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("janani", "Janani23092004")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.accountNumber").value("APXAC00001"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Smith"))
                .andExpect(jsonPath("$.balance").value(45250.00))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    public void testTransfer_Success() throws Exception {
        TransferRequest request = new TransferRequest(
                "APXAC00001", // John Smith (45250.00)
                "APXAC00002", // Jane Doe (10000.00)
                new BigDecimal("250.00"),
                UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/api/v1/transfers")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("janani", "Janani23092004"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Transfer completed successfully"))
                .andExpect(jsonPath("$.debitedFrom").value("APXAC00001"))
                .andExpect(jsonPath("$.creditedTo").value("APXAC00002"))
                .andExpect(jsonPath("$.amount").value(250.00));
    }

    @Test
    public void testTransfer_InsufficientFunds() throws Exception {
        TransferRequest request = new TransferRequest(
                "APXAC00004", // Alice Brown (750.00)
                "APXAC00002", // Jane Doe (10000.00)
                new BigDecimal("1000.00"), // More than 750
                UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/api/v1/transfers")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("janani", "Janani23092004"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("TRX-400"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Insufficient funds")));
    }

    @Test
    public void testReopenAccount_Within15Days_ThrowsException() throws Exception {
        // Arrange
        Account acc = accountRepository.findByAccountNumber("APXAC00001").orElseThrow();
        acc.setBalance(BigDecimal.ZERO);
        acc.setStatus(com.banking.moneytransfer.domain.AccountStatus.CLOSED);
        acc.setClosureReason("Test closure");
        accountRepository.saveAndFlush(acc);

        // Act & Assert
        com.banking.moneytransfer.dto.CreateAccountRequest request = new com.banking.moneytransfer.dto.CreateAccountRequest(
                com.banking.moneytransfer.domain.AccountType.SAVINGS,
                new BigDecimal("2000.00"),
                "AADHAAR_CARD",
                "1234567890"
        );

        mockMvc.perform(post("/api/v1/accounts")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("janani", "Janani23092004"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Cannot reopen account within 15 days of closure of account"));
    }

    @Test
    public void testDownloadStatement_Success() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/APXAC00001/statement/pdf")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("janani", "Janani23092004")))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Content-Type", "application/pdf"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Content-Disposition", org.hamcrest.Matchers.containsString("statement-APXAC00001.pdf")));
    }

    @Test
    public void testDownloadStatement_Forbidden() throws Exception {
        // janani attempts to download jane's account statement (APXAC00002)
        mockMvc.perform(get("/api/v1/accounts/APXAC00002/statement/pdf")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("janani", "Janani23092004")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testDownloadStatement_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/APXAC00001/statement/pdf"))
                .andExpect(status().isUnauthorized());
    }
}
