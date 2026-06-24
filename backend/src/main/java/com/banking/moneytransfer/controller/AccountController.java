package com.banking.moneytransfer.controller;

import com.banking.moneytransfer.domain.Account;
import com.banking.moneytransfer.domain.User;
import com.banking.moneytransfer.dto.AccountResponse;
import com.banking.moneytransfer.dto.CreateAccountRequest;
import com.banking.moneytransfer.repository.UserRepository;
import com.banking.moneytransfer.service.AccountService;
import com.banking.moneytransfer.service.PdfGeneratorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/accounts")
@CrossOrigin(origins = "*") 
public class AccountController {

    private final AccountService accountService;
    private final UserRepository userRepository;
    private final PdfGeneratorService pdfGeneratorService;

    @Autowired
    public AccountController(AccountService accountService, UserRepository userRepository, PdfGeneratorService pdfGeneratorService) {
        this.accountService = accountService;
        this.userRepository = userRepository;
        this.pdfGeneratorService = pdfGeneratorService;
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountNumber) {
        Account account = accountService.getAccount(accountNumber);
        return ResponseEntity.ok(mapToResponse(account));
    }

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<Map<String, Object>> getBalance(@PathVariable String accountNumber) {
        BigDecimal balance = accountService.getBalance(accountNumber);
        Map<String, Object> response = new HashMap<>();
        response.put("accountNumber", accountNumber);
        response.put("balance", balance);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountNumber}/transactions")
    public ResponseEntity<List<com.banking.moneytransfer.dto.TransactionLogResponse>> getTransactions(@PathVariable String accountNumber) {
        List<com.banking.moneytransfer.dto.TransactionLogResponse> logs = accountService.getTransactions(accountNumber);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/{accountNumber}/statement/pdf")
    public ResponseEntity<byte[]> downloadStatement(Principal principal, @PathVariable String accountNumber) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + principal.getName()));

        Account account = accountService.getAccount(accountNumber);
        
        // Secure check: verify requester owns the account
        if (!account.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        List<com.banking.moneytransfer.dto.TransactionLogResponse> logs = accountService.getTransactions(accountNumber);
        java.io.ByteArrayInputStream pdfStream = pdfGeneratorService.generateStatementPdf(account, logs);

        byte[] pdfBytes = pdfStream.readAllBytes();

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment()
                .filename("statement-" + accountNumber + ".pdf")
                .build());

        return new ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);
    }

    @GetMapping("/{accountNumber}/verify")
    public ResponseEntity<Map<String, Object>> verifyAccount(@PathVariable String accountNumber) {
        Account account = accountService.getAccount(accountNumber);
        Map<String, Object> response = new HashMap<>();
        response.put("accountNumber", account.getAccountNumber());
        response.put("accountHolderName", account.getFirstName() + " " + account.getLastName());
        response.put("isValid", true);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<AccountResponse> openAccount(Principal principal, @Valid @RequestBody CreateAccountRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + principal.getName()));

        Account account = accountService.openAccount(user, request.accountType(), request.initialDeposit());
        return ResponseEntity.ok(mapToResponse(account));
    }

    @PatchMapping("/{accountNumber}/close")
    public ResponseEntity<AccountResponse> closeAccount(Principal principal, @PathVariable String accountNumber, @RequestBody Map<String, String> body) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + principal.getName()));

        String reason = body.getOrDefault("reason", "No reason specified");
        Account closedAccount = accountService.closeAccount(accountNumber, reason, user);
        return ResponseEntity.ok(mapToResponse(closedAccount));
    }

    private AccountResponse mapToResponse(Account account) {
        return new AccountResponse(
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
        );
    }
}
