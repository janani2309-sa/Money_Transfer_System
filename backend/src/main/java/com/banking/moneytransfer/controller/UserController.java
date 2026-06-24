package com.banking.moneytransfer.controller;

import com.banking.moneytransfer.domain.User;
import com.banking.moneytransfer.dto.AccountResponse;
import com.banking.moneytransfer.dto.UserResponse;
import com.banking.moneytransfer.repository.UserRepository;
import com.banking.moneytransfer.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    @Autowired
    public UserController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + principal.getName()));

        List<AccountResponse> accountResponses = user.getAccounts().stream()
                .map(account -> new AccountResponse(
                        account.getId(),
                        account.getAccountNumber(),
                        account.getAccountType(),
                        account.getFirstName(),
                        account.getLastName(),
                        account.getBalance(),
                        account.getStatus(),
                        account.getOpenedDate(),
                        account.getLastUpdated(),
                        account.getClosureReason()
                ))
                .toList();

        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                accountResponses
        );

        return ResponseEntity.ok(userResponse);
    }

    @DeleteMapping("/profile")
    public ResponseEntity<Map<String, Object>> deleteUserProfile(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + principal.getName()));

        userService.deleteUserProfile(user);

        SecurityContextHolder.clearContext();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Profile successfully deleted and anonymized.");
        return ResponseEntity.ok(response);
    }
}
