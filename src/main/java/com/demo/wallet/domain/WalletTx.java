package com.demo.wallet.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "wallet_tx",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_idempo", columnNames = "idempotency_key")
    },
    indexes = {
        @Index(name = "idx_user_created", columnList = "user_id, created_at")
    }
)
public class WalletTx {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tx_id", nullable = false)
    private Long txId;

    // FK이지만 데모에서는 연관관계(ManyToOne) 대신 userId만 들고 가는 게 단순함
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TxType type;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TxStatus status;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "balance_after")
    private Long balanceAfter;

    protected WalletTx() {
        // JPA 기본 생성자
    }

    public WalletTx(Long userId, TxType type, Long amount, TxStatus status, String idempotencyKey, Long balanceAfter) {
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.balanceAfter = balanceAfter;
    }

    public void markSuccess() {
        this.status = TxStatus.SUCCESS;
    }

    public void markFailed() {
        this.status = TxStatus.FAILED;
    }
    
    public Long getBalanceAfter() {
        return balanceAfter;
    }

    // ===== getter =====
    public Long getTxId() { return txId; }
    public Long getUserId() { return userId; }
    public TxType getType() { return type; }
    public Long getAmount() { return amount; }
    public TxStatus getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
