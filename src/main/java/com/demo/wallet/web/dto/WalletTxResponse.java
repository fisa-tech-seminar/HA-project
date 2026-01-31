package com.demo.wallet.web.dto;

import java.time.LocalDateTime;

import com.demo.wallet.domain.TxStatus;
import com.demo.wallet.domain.TxType;

public record WalletTxResponse(
        Long txId,
        Long userId,
        String idempotencyKey,
        TxType type,
        Long amount,
        TxStatus status,
        Long balanceAfter,
        LocalDateTime createdAt
) { }
