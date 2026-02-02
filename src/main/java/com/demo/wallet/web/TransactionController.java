package com.demo.wallet.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.demo.wallet.domain.WalletTx;
import com.demo.wallet.repository.WalletTxRepository;
import com.demo.wallet.web.dto.ApiResponse;
import com.demo.wallet.web.dto.WalletTxResponse;

@RestController
@RequestMapping("/api/v1")
public class TransactionController {

	private final WalletTxRepository walletTxRepository;

    public TransactionController(WalletTxRepository walletTxRepository) {
        this.walletTxRepository = walletTxRepository;
    }

    @GetMapping("/transactions/{idempotencyKey}")
    public ApiResponse<WalletTxResponse> getTx(@PathVariable("idempotencyKey") String idempotencyKey) {
        WalletTx tx = walletTxRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));
        return ApiResponse.ok(new WalletTxResponse(
                tx.getTxId(),
                tx.getUserId(),
                tx.getIdempotencyKey(),
                tx.getType(),
                tx.getAmount(),
                tx.getStatus(),
                tx.getBalanceAfter(),
                tx.getCreatedAt()
        ));
    }
}
