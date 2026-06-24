package com.banking.moneytransfer.controller;

import com.banking.moneytransfer.domain.Account;
import com.banking.moneytransfer.domain.AccountType;
import com.banking.moneytransfer.domain.User;
import com.banking.moneytransfer.dto.SignupRequest;
import com.banking.moneytransfer.dto.VerifyOtpRequest;
import com.banking.moneytransfer.repository.UserRepository;
import com.banking.moneytransfer.service.AccountService;
import com.banking.moneytransfer.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final SecureRandom secureRandom = new SecureRandom();

    private final UserRepository userRepository;
    private final AccountService accountService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Autowired
    public AuthController(UserRepository userRepository, AccountService accountService, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.accountService = accountService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(@Valid @RequestBody SignupRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        String otpCode = String.format("%06d", secureRandom.nextInt(1000000));
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);

        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .phoneNumber(request.phoneNumber())
                .isVerified(false)
                .otpCode(otpCode)
                .otpExpiry(expiry)
                .build();

        userRepository.save(user);

        // Send Email
        emailService.sendOtpEmail(request.email(), otpCode);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "PENDING_OTP");
        response.put("username", request.username());
        response.put("message", "OTP sent successfully to " + request.email());
        response.put("debugOtp", otpCode); 

        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.isVerified()) {
            throw new IllegalArgumentException("User is already verified");
        }

        if (user.getOtpCode() == null || !user.getOtpCode().equals(request.otp())) {
            throw new IllegalArgumentException("Invalid OTP code");
        }

        if (user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("OTP code has expired. Please request a new one.");
        }

        // Mark user as verified and clear OTP
        user.setVerified(true);
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        // Automatically create a default SAVINGS account with 1000.00 Rs
        Account defaultAccount = accountService.openAccount(user, AccountType.SAVINGS, new BigDecimal("1000.00"));

        Map<String, Object> response = new HashMap<>();
        response.put("status", "VERIFIED");
        response.put("username", user.getUsername());
        response.put("accountNumber", defaultAccount.getAccountNumber());
        response.put("message", "User verified successfully. Default SAVINGS account created.");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<Map<String, Object>> resendOtp(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.isVerified()) {
            throw new IllegalArgumentException("User is already verified");
        }

        String otpCode = String.format("%06d", secureRandom.nextInt(1000000));
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);

        user.setOtpCode(otpCode);
        user.setOtpExpiry(expiry);
        userRepository.save(user);

        emailService.sendOtpEmail(user.getEmail(), otpCode);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "PENDING_OTP");
        response.put("username", user.getUsername());
        response.put("message", "A new OTP has been sent successfully.");
        response.put("debugOtp", otpCode);

        return ResponseEntity.ok(response);
    }
}
