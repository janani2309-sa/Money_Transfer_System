package com.banking.moneytransfer.controller;

import com.banking.moneytransfer.dto.TransferRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
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

    @Test
    public void testGetAccount_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetAccount_Success() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/1")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("janani", "Janani23092004")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.holderName").value("John Smith"))
                .andExpect(jsonPath("$.balance").value(45250.00))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    public void testTransfer_Success() throws Exception {
        TransferRequest request = new TransferRequest(
                1L, // John Smith (45250.00)
                2L, // Jane Doe (10000.00)
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
                .andExpect(jsonPath("$.debitedFrom").value(1))
                .andExpect(jsonPath("$.creditedTo").value(2))
                .andExpect(jsonPath("$.amount").value(250.00));
    }

    @Test
    public void testTransfer_InsufficientFunds() throws Exception {
        TransferRequest request = new TransferRequest(
                4L, // Alice Brown (750.00)
                2L, // Jane Doe (10000.00)
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
}
