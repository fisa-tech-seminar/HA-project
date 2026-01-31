package com.demo.wallet.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="wallet")
public class Wallet {
	
	@Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "balance", nullable = false)
    private Long balance;

    // 비관적 락을 쓸 거라 @Version은 필수는 아니지만,
    // 향후 낙관적 락 전환/추적 용도로 남겨둬도 됨.
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected Wallet() {
        // JPA 기본 생성자
    }

    public Wallet(Long userId) {
        this.userId = userId;
        this.balance = 0L;
        this.version = 0;
    }

    // ===== 비즈니스 메서드 =====
    public void topup(long amount) {
        this.balance += amount;
    }

    public void pay(long amount) {
        this.balance -= amount;
    }

    public boolean canPay(long amount) {
        return this.balance >= amount;
    }

    // ===== getter =====
    public Long getUserId() { return userId; }
    public Long getBalance() { return balance; }
    public Integer getVersion() { return version; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

}
