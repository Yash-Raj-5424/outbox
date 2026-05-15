package com.pay.outbox.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
public class LedgerService {

    private final RedisTemplate<String, String> redisTemplate;
    private final DefaultRedisScript<Long> holdBalanceScript;
    private final DefaultRedisScript<Long> releaseBalanceScript;

    public LedgerService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;

        this.holdBalanceScript = new DefaultRedisScript<>();
        this.holdBalanceScript.setScriptSource(
                new ResourceScriptSource(new ClassPathResource("lua/holdBalance.lua")));
        this.holdBalanceScript.setResultType(Long.class);

        this.releaseBalanceScript = new DefaultRedisScript<>();
        this.releaseBalanceScript.setScriptSource(
                new ResourceScriptSource(new ClassPathResource("lua/releaseBalance.lua")));
        this.releaseBalanceScript.setResultType(Long.class);
    }

    public boolean holdBalance(String accountId, Long amount) {
        String key = "ledger:" + accountId;
        log.info("Attempting to hold {} for account {}", amount, accountId);

        Long result = redisTemplate.execute(
                holdBalanceScript,
                Collections.singletonList(key),
                String.valueOf(amount)
        );

        if (result == null || result == -1L) {
            log.warn("Account {} not found in ledger", accountId);
            return false;
        }

        if (result == -2L) {
            log.warn("Insufficient balance for account {} to hold {}", accountId, amount);
            return false;
        }

        log.info("Hold successful for account {}. Remaining balance: {}", accountId, result);
        return true;
    }

    public void releaseBalance(String accountId, Long amount) {
        String key = "ledger:" + accountId;
        log.info("Releasing {} for account {}", amount, accountId);

        Long result = redisTemplate.execute(
                releaseBalanceScript,
                Collections.singletonList(key),
                String.valueOf(amount)
        );

        log.info("Release successful for account {}. Remaining balance: {}", accountId, result);
    }

    public void seedBalance(String accountId, Long amount) {
        String key = "ledger:" + accountId;
        redisTemplate.opsForValue().set(key, String.valueOf(amount));
        log.info("Seeded balance {} for account {}", amount, accountId);
    }

    public Long getBalance(String accountId) {
        String key = "ledger:" + accountId;
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : null;
    }
}