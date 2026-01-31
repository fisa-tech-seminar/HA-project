package com.demo.wallet.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.demo.wallet.domain.WalletTx;

public interface WalletTxRepository extends JpaRepository<WalletTx, Long> {

	/**
     * 멱등 처리 핵심: 같은 Idempotency-Key로 들어온 요청은 동일 결과를 반환한다.
     */
    Optional<WalletTx> findByIdempotencyKey(String idempotencyKey);
}

