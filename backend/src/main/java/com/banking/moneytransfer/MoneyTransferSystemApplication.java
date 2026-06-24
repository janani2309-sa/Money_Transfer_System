package com.banking.moneytransfer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MoneyTransferSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(MoneyTransferSystemApplication.class, args);
    }
}

