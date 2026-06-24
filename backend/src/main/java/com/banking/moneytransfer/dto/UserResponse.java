package com.banking.moneytransfer.dto;

import java.util.List;

public record UserResponse(
    Long id,
    String username,
    String firstName,
    String lastName,
    String email,
    String phoneNumber,
    List<AccountResponse> accounts
) {}
