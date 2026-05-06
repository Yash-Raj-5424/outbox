package com.pay.outbox.repository;

import com.pay.outbox.domain.entity.Payout;
import com.pay.outbox.domain.enums.PayoutStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, UUID> {

    Optional<Payout> findByIdempotencyKey(String idempotencyKey);

    List<Payout> findByStatus(PayoutStatus status);
}