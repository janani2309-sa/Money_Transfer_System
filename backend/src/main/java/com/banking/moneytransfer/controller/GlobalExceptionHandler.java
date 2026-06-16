package com.banking.moneytransfer.controller;

import com.banking.moneytransfer.dto.ErrorResponse;
import com.banking.moneytransfer.exception.BankingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BankingException.class)
    public ResponseEntity<ErrorResponse> handleBankingException(BankingException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST; // default

        switch (ex.getErrorCode()) {
            case "ACC-404":
                status = HttpStatus.NOT_FOUND;
                break;
            case "ACC-403":
                status = HttpStatus.FORBIDDEN;
                break;
            case "TRX-400":
                status = HttpStatus.BAD_REQUEST;
                break;
            case "TRX-409":
                status = HttpStatus.CONFLICT;
                break;
        }

        ErrorResponse error = new ErrorResponse(ex.getErrorCode(), ex.getMessage());
        return new ResponseEntity<>(error, status);
    }

    /**
     * Handles JSR-380 Bean validation failures (e.g. from TransferRequest annotations).
     * Maps to VAL-422 and HTTP 422.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        ErrorResponse error = new ErrorResponse("VAL-422", "Validation failed: " + details);
        return new ResponseEntity<>(error, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * Handles invalid arguments like identical source and destination accounts.
     * Maps to VAL-422 and HTTP 422.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse("VAL-422", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * Handles Optimistic Locking concurrency failures.
     * Maps to CONCURRENCY-409 and HTTP 409.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(ObjectOptimisticLockingFailureException ex) {
        ErrorResponse error = new ErrorResponse("TRX-409", "Conflict detected: The account has been updated by another transaction. Please try again.");
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    /**
     * Handles generic exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse error = new ErrorResponse("SYS-500", "An unexpected system error occurred: " + ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
