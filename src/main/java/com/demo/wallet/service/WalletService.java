package com.demo.wallet.service;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demo.wallet.domain.TxStatus;
import com.demo.wallet.domain.TxType;
import com.demo.wallet.domain.Wallet;
import com.demo.wallet.domain.WalletTx;
import com.demo.wallet.repository.WalletRepository;
import com.demo.wallet.repository.WalletTxRepository;



@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTxRepository walletTxRepository;

    public WalletService(WalletRepository walletRepository, WalletTxRepository walletTxRepository) {
        this.walletRepository = walletRepository;
        this.walletTxRepository = walletTxRepository;
    }

    @Transactional
    public long getBalance(Long userId) {
        Wallet wallet = walletRepository.findById(userId)
                .orElseGet(() -> walletRepository.save(new Wallet(userId)));
        return wallet.getBalance();
    }

    @Transactional
    public WalletTxResult topup(Long userId, long amount, String idempotencyKey) {
        validateInputs(userId, amount, idempotencyKey);

        // 1) 멱등 처리: 이미 처리된 키면 저장된 결과 그대로 반환
        Optional<WalletTx> existing = walletTxRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return toResult(existing.get());
        }

        // 2) 지갑 row 락 획득 (없으면 생성 후 다시 락 조회)
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> {
                    walletRepository.save(new Wallet(userId));
                    return walletRepository.findByUserIdForUpdate(userId)
                            .orElseThrow(() -> new IllegalStateException("Wallet creation failed"));
                });

        // 3) 잔액 업데이트 먼저 반영 (balance_after를 정확히 저장하기 위함)
        wallet.topup(amount);
        walletRepository.save(wallet);

        long balanceAfter = wallet.getBalance();

        // 4) wallet_tx 저장 (UNIQUE 충돌이면 재조회하여 반환)
        WalletTx tx = new WalletTx(userId, TxType.TOPUP, amount, TxStatus.SUCCESS, idempotencyKey, balanceAfter);
        WalletTx savedTx = saveTxIdempotently(tx);

        return toResult(savedTx);
    }

    @Transactional
    public WalletTxResult pay(Long userId, long amount, String idempotencyKey) {
        validateInputs(userId, amount, idempotencyKey);

        // 1) 멱등 처리
        Optional<WalletTx> existing = walletTxRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return toResult(existing.get());
        }

        // 2) 지갑 row 락
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> {
                    walletRepository.save(new Wallet(userId));
                    return walletRepository.findByUserIdForUpdate(userId)
                            .orElseThrow(() -> new IllegalStateException("Wallet creation failed"));
                });

        // 3) 잔액 체크
        if (!wallet.canPay(amount)) {
            long balanceAfter = wallet.getBalance();

            // 실패도 원장에 남김 (balance_after는 현재 잔액 그대로)
            WalletTx failedTx = new WalletTx(userId, TxType.PAY, amount, TxStatus.FAILED, idempotencyKey, balanceAfter);
            saveTxIdempotently(failedTx);

            throw new InsufficientBalanceException(
                    "Insufficient balance. balance=" + wallet.getBalance() + ", amount=" + amount
            );
        }

        // 4) 잔액 차감 먼저 반영
        wallet.pay(amount);
        walletRepository.save(wallet);

        long balanceAfter = wallet.getBalance();

        // 5) 성공 tx 저장
        WalletTx tx = new WalletTx(userId, TxType.PAY, amount, TxStatus.SUCCESS, idempotencyKey, balanceAfter);
        WalletTx savedTx = saveTxIdempotently(tx);

        return toResult(savedTx);
    }

    private void validateInputs(Long userId, long amount, String idempotencyKey) {
        if (userId == null || userId <= 0) {
            throw new InvalidRequestException("userId must be positive");
        }
        if (amount <= 0) {
            throw new InvalidRequestException("amount must be positive");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 64) {
            throw new InvalidRequestException("Idempotency-Key is required and must be <= 64 chars");
        }
    }

    /**
     * idempotency_key UNIQUE 충돌은 "동일 요청 동시 도착" 또는 "재시도"로 간주하고,
     * 기존 레코드를 재조회하여 반환한다.
     */
    private WalletTx saveTxIdempotently(WalletTx tx) {
    	try {
            return walletTxRepository.saveAndFlush(tx); // ✅ flush까지 해서 여기서 예외를 잡는다
        } catch (DataIntegrityViolationException e) {
            return walletTxRepository.findByIdempotencyKey(tx.getIdempotencyKey())
                    .orElseThrow(() -> e);
        }
    }

    private WalletTxResult toResult(WalletTx tx) {
        return new WalletTxResult(
                tx.getTxId(),
                tx.getUserId(),
                tx.getIdempotencyKey(),
                tx.getType(),
                tx.getAmount(),
                tx.getStatus(),
                tx.getBalanceAfter(),  // ✅ 이제 tx에 저장된 값을 그대로 사용
                tx.getCreatedAt()
        );
    }
}
