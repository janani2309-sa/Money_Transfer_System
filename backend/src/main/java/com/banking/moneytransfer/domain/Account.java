package com.banking.moneytransfer.domain;

import com.banking.moneytransfer.exception.InsufficientBalanceException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true, length = 36)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Version
    @Column(nullable = false)
    private Integer version;

    @Column(name = "closure_reason", length = 255)
    private String closureReason;

    @CreationTimestamp
    @Column(name = "opened_date", nullable = false, updatable = false)
    private LocalDateTime openedDate;

    @UpdateTimestamp
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    /**
     * Checks if the account status is ACTIVE.
     */
    public boolean isActive() {
        return AccountStatus.ACTIVE.equals(this.status);
    }

    /**
     * Debits (deducts) an amount from the account balance.
     * Throws InsufficientBalanceException if balance is insufficient.
     */
    public void debit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be greater than zero");
        }
        BigDecimal remaining = this.balance.subtract(amount);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientBalanceException("Insufficient funds. Transaction declined.");
        }
        if (remaining.compareTo(BigDecimal.ZERO) > 0 && remaining.compareTo(new BigDecimal("1000.00")) < 0) {
            throw new InsufficientBalanceException("Insufficient funds. Transaction declined. Account balance cannot fall below the minimum balance of 1000.00 Rs.");
        }
        this.balance = remaining;
    }

    /**
     * Credits (adds) an amount to the account balance.
     */
    public void credit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be greater than zero");
        }
        BigDecimal newBalance = this.balance.add(amount);
        if (newBalance.compareTo(new BigDecimal("10000000.00")) > 0) {
            throw new IllegalArgumentException("Transaction declined. Account balance cannot exceed the maximum balance limit of 10,000,000.00 Rs.");
        }
        this.balance = newBalance;
    }
}
