package com.banking.moneytransfer.exception;

public final class DuplicateTransferException extends BankingException {
    public DuplicateTransferException(String message) {
        super(message, "TRX-409");
    }
}
