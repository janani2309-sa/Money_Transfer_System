package com.banking.moneytransfer.controller;

import com.banking.moneytransfer.domain.Account;
import com.banking.moneytransfer.domain.AccountStatus;
import com.banking.moneytransfer.domain.AccountType;
import com.banking.moneytransfer.domain.User;
import com.banking.moneytransfer.repository.AccountRepository;
import com.banking.moneytransfer.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

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
    public void testGetCurrentUser_Success() throws Exception {
        mockMvc.perform(get("/api/v1/users/profile")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("testuser", "Password123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.accounts[0].accountNumber").value("APXAC12345"));
    }

    @Test
    public void testGetCurrentUser_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testDeleteUserProfile_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/users/profile")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("testuser", "Password123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Profile successfully deleted and anonymized."));
    }

    @Test
    public void testDeleteUserProfile_Unauthorized() throws Exception {
        mockMvc.perform(delete("/api/v1/users/profile"))
                .andExpect(status().isUnauthorized());
    }
}
