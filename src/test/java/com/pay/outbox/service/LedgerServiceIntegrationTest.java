package com.pay.outbox.service;

import com.pay.outbox.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class LedgerServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private LedgerService ledgerService;

    @BeforeEach
    void setUp(){
        ledgerService.seedBalance("ledger_test_user", 100000L);
    }

    @Test
    void shouldHoldBalanceSuccessfully(){
        boolean held = ledgerService.holdBalance("ledger_test_user", 30000L);
        assertThat(held).isTrue();
        assertThat(ledgerService.getBalance("ledger_test_user")).isEqualTo(70000L);
    }

    @Test
    void shouldRejectHoldWhenInsufficientBalance(){
        boolean held = ledgerService.holdBalance("ledger_test_user", 999999L);
        assertThat(held).isFalse();
        assertThat(ledgerService.getBalance("ledger_test_user")).isEqualTo(100000L);
    }

    @Test
    void shouldReleaseBalanceSuccessfully(){
        ledgerService.holdBalance("ledger_test_user", 30000L);
        ledgerService.releaseBalance("ledger_test_user", 30000L);
        assertThat(ledgerService.getBalance("ledger_test_user")).isEqualTo(100000L);
    }

    @Test
    void shouldReturnNullForUnknownAccount(){
        Long balance = ledgerService.getBalance("unknown_user");
        assertThat(balance).isNull();
    }
}