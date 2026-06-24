package com.banking.moneytransfer.service;

import com.banking.moneytransfer.domain.Account;
import com.banking.moneytransfer.domain.AccountStatus;
import com.banking.moneytransfer.domain.User;
import com.banking.moneytransfer.repository.AccountRepository;
import com.banking.moneytransfer.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    @Autowired
    public UserService(UserRepository userRepository, AccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }


    @Transactional
    public void deleteUserProfile(User user) {
        for (Account account : user.getAccounts()) {
            account.setStatus(AccountStatus.CLOSED);
            account.setClosureReason("User profile deleted");
            account.setBalance(BigDecimal.ZERO);
            accountRepository.save(account);
        }

        String uniqueSuffix = "_" + user.getId();
        user.setFirstName("Deleted");
        user.setLastName("User");
        user.setUsername("deleted" + uniqueSuffix);
        user.setEmail("deleted" + uniqueSuffix + "@apexpay.com");
        user.setPhoneNumber("deleted" + uniqueSuffix);
        user.setPassword(""); 
        user.setVerified(false);

        userRepository.save(user);
    }
}
