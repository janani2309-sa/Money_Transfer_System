package com.banking.moneytransfer.controller;

import com.banking.moneytransfer.domain.Account;
import com.banking.moneytransfer.exception.AccountNotActiveException;
import com.banking.moneytransfer.exception.AccountNotFoundException;
import com.banking.moneytransfer.exception.DuplicateTransferException;
import com.banking.moneytransfer.exception.InsufficientBalanceException;
import com.banking.moneytransfer.service.AccountService;
import com.banking.moneytransfer.domain.User;
import com.banking.moneytransfer.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class GlobalExceptionHandlerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @Autowired
    private UserRepository userRepository;

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
                .username("janani")
                .password(encoder.encode("Janani23092004"))
                .firstName("John")
                .lastName("Doe")
                .email("janani@example.com")
                .phoneNumber("1234567890")
                .isVerified(true)
                .build();
        userRepository.save(user);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    public void testHandleBankingException_AccountNotFound() throws Exception {
        when(accountService.getBalance(anyString()))
                .thenThrow(new AccountNotFoundException("Account not found custom message"));

        mockMvc.perform(get("/api/v1/accounts/APXAC99999/balance")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("janani", "Janani23092004")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ACC-404"))
                .andExpect(jsonPath("$.message").value("Account not found custom message"));
    }

    @Test
    public void testHandleBankingException_AccountNotActive() throws Exception {
        when(accountService.getBalance(anyString()))
                .thenThrow(new AccountNotActiveException("Account is inactive"));

        mockMvc.perform(get("/api/v1/accounts/APXAC99999/balance")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("janani", "Janani23092004")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACC-403"))
                .andExpect(jsonPath("$.message").value("Account is inactive"));
    }

    @Test
    public void testHandleBankingException_DuplicateTransfer() throws Exception {
        when(accountService.getBalance(anyString()))
                .thenThrow(new DuplicateTransferException("Duplicate transaction"));

        mockMvc.perform(get("/api/v1/accounts/APXAC99999/balance")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("janani", "Janani23092004")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("TRX-409"))
                .andExpect(jsonPath("$.message").value("Duplicate transaction"));
    }

    @Test
    public void testHandleBankingException_InsufficientBalance() throws Exception {
        when(accountService.getBalance(anyString()))
                .thenThrow(new InsufficientBalanceException("Insufficient funds"));

        mockMvc.perform(get("/api/v1/accounts/APXAC99999/balance")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("janani", "Janani23092004")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("TRX-400"))
                .andExpect(jsonPath("$.message").value("Insufficient funds"));
    }

    @Test
    public void testHandleOptimisticLockingFailure() throws Exception {
        when(accountService.getBalance(anyString()))
                .thenThrow(new ObjectOptimisticLockingFailureException(Account.class, "APXAC99999"));

        mockMvc.perform(get("/api/v1/accounts/APXAC99999/balance")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("janani", "Janani23092004")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("TRX-409"))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("Conflict detected")));
    }

    @Test
    public void testHandleGenericException() throws Exception {
        when(accountService.getBalance(anyString()))
                .thenThrow(new RuntimeException("Oops unexpected error"));

        mockMvc.perform(get("/api/v1/accounts/APXAC99999/balance")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("janani", "Janani23092004")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("SYS-500"))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("Oops unexpected error")));
    }

    @Test
    public void testHandleValidationExceptions() throws Exception {
        // Send request body with null accountType to trigger MethodArgumentNotValidException
        String invalidPayload = "{\"accountType\":null,\"initialDeposit\":-50.00,\"documentType\":\"\",\"documentNumber\":\"\"}";

        mockMvc.perform(post("/api/v1/accounts")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("janani", "Janani23092004"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("VAL-422"))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("Validation failed")));
    }
}
