package com.banking.moneytransfer.controller;

import com.banking.moneytransfer.domain.User;
import com.banking.moneytransfer.dto.SignupRequest;
import com.banking.moneytransfer.dto.VerifyOtpRequest;
import com.banking.moneytransfer.repository.UserRepository;
import com.banking.moneytransfer.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private EmailService emailService;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @BeforeEach
    public void setUp() {
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE transaction_logs").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE accounts").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE users").executeUpdate();
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();

        doNothing().when(emailService).sendOtpEmail(anyString(), anyString());
    }

    @Test
    public void testSignup_Success() throws Exception {
        SignupRequest request = new SignupRequest(
                "newuser",
                "Password123!",
                "John",
                "Doe",
                "john.doe@example.com",
                "1234567890"
        );

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_OTP"))
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.message").value("OTP sent successfully to john.doe@example.com"));
    }

    @Test
    public void testSignup_ConflictUsername() throws Exception {
        User existing = User.builder()
                .username("existinguser")
                .password("encodedpassword")
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@example.com")
                .phoneNumber("0987654321")
                .isVerified(false)
                .build();
        userRepository.save(existing);

        SignupRequest request = new SignupRequest(
                "existinguser",
                "Password123!",
                "John",
                "Doe",
                "john.doe@example.com",
                "1234567890"
        );

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void testVerifyOtp_Success() throws Exception {
        User user = User.builder()
                .username("testuser")
                .password("encodedpassword")
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .phoneNumber("1234567890")
                .isVerified(false)
                .otpCode("123456")
                .otpExpiry(LocalDateTime.now().plusMinutes(5))
                .build();
        userRepository.save(user);

        VerifyOtpRequest request = new VerifyOtpRequest("testuser", "123456");

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    public void testVerifyOtp_UserNotFound() throws Exception {
        VerifyOtpRequest request = new VerifyOtpRequest("nonexistent", "123456");

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void testVerifyOtp_AlreadyVerified() throws Exception {
        User user = User.builder()
                .username("testuser")
                .password("encodedpassword")
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .phoneNumber("1234567890")
                .isVerified(true)
                .build();
        userRepository.save(user);

        VerifyOtpRequest request = new VerifyOtpRequest("testuser", "123456");

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void testVerifyOtp_InvalidOtp() throws Exception {
        User user = User.builder()
                .username("testuser")
                .password("encodedpassword")
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .phoneNumber("1234567890")
                .isVerified(false)
                .otpCode("123456")
                .otpExpiry(LocalDateTime.now().plusMinutes(5))
                .build();
        userRepository.save(user);

        VerifyOtpRequest request = new VerifyOtpRequest("testuser", "999999");

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void testVerifyOtp_ExpiredOtp() throws Exception {
        User user = User.builder()
                .username("testuser")
                .password("encodedpassword")
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .phoneNumber("1234567890")
                .isVerified(false)
                .otpCode("123456")
                .otpExpiry(LocalDateTime.now().minusMinutes(5))
                .build();
        userRepository.save(user);

        VerifyOtpRequest request = new VerifyOtpRequest("testuser", "123456");

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void testResendOtp_Success() throws Exception {
        User user = User.builder()
                .username("testuser")
                .password("encodedpassword")
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .phoneNumber("1234567890")
                .isVerified(false)
                .build();
        userRepository.save(user);

        Map<String, String> request = new HashMap<>();
        request.put("username", "testuser");

        mockMvc.perform(post("/api/v1/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_OTP"));
    }

    @Test
    public void testResendOtp_UsernameRequired() throws Exception {
        Map<String, String> request = new HashMap<>();

        mockMvc.perform(post("/api/v1/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void testResendOtp_UserNotFound() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("username", "nonexistent");

        mockMvc.perform(post("/api/v1/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void testResendOtp_AlreadyVerified() throws Exception {
        User user = User.builder()
                .username("testuser")
                .password("encodedpassword")
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .phoneNumber("1234567890")
                .isVerified(true)
                .build();
        userRepository.save(user);

        Map<String, String> request = new HashMap<>();
        request.put("username", "testuser");

        mockMvc.perform(post("/api/v1/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }
}
