package com.pay.outbox.controller;

import com.pay.outbox.dto.PayoutRequest;
import com.pay.outbox.dto.PayoutResponse;
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
}