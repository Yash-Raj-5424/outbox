package com.pay.outbox.controller;

import com.pay.outbox.dto.PayoutRequest;
import com.pay.outbox.dto.PayoutResponse;
import com.pay.outbox.service.LedgerService;
import com.pay.outbox.service.PayoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/payouts")
@RequiredArgsConstructor
public class PayoutController {

    private final PayoutService payoutService;
    private final LedgerService ledgerService;

    @PostMapping
    public ResponseEntity<PayoutResponse> initiatePayout(@Valid @RequestBody PayoutRequest request) {
        log.info("Received payout request for recipient: {}", request.getRecipientId());
        PayoutResponse response = payoutService.initiatePayout(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PayoutResponse> getPayoutById(@PathVariable UUID id) {
        log.info("Fetching payout with id: {}", id);
        PayoutResponse response = payoutService.getPayoutById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/ledger/seed")
    public ResponseEntity<String> seedBalance(@RequestParam String accountId,
                                              @RequestParam Long amount) {
        ledgerService.seedBalance(accountId, amount);
        return ResponseEntity.ok("Balance seeded: " + amount + " for account: " + accountId);
    }

    @GetMapping("/ledger/balance/{accountId}")
    public ResponseEntity<Long> getBalance(@PathVariable String accountId) {
        Long balance = ledgerService.getBalance(accountId);
        if (balance == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(balance);
    }
}