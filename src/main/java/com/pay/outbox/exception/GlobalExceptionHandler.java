package com.pay.outbox.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String BASE_URL = "https://outbox-payments.com/errors";

    @ExceptionHandler(PayoutNotFoundException.class)
    public ProblemDetail handlePayoutNotFound(PayoutNotFoundException ex) {
        log.warn("Payout not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Payout Not Found");
        problem.setType(URI.create(BASE_URL + "/payout-not-found"));
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(field, message);
        });
        log.warn("Validation failed: {}", errors);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "One or more fields are invalid");
        problem.setTitle("Validation Failed");
        problem.setType(URI.create(BASE_URL + "/validation-failed"));
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred");
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create(BASE_URL + "/internal-server-error"));
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed or missing request body: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Request body is missing or malformed");
        problem.setTitle("Malformed Request");
        problem.setType(URI.create(BASE_URL + "/malformed-request"));
        return problem;
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ProblemDetail handleInsufficientBalance(InsufficientBalanceException ex) {
        log.warn("Insufficient balance: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Insufficient Balance");
        problem.setType(URI.create(BASE_URL + "/insufficient-balance"));
        return problem;
    }

}