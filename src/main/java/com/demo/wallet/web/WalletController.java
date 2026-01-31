package com.demo.wallet.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.demo.wallet.service.WalletService;
import com.demo.wallet.service.WalletTxResult;
import com.demo.wallet.web.dto.AmountRequest;
import com.demo.wallet.web.dto.ApiResponse;
import com.demo.wallet.web.dto.WalletBalanceResponse;
import com.demo.wallet.web.dto.WalletTxResponse;

@RestController
@RequestMapping("/api/v1")
public class WalletController {

	private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/wallets/{userId}")
    public ApiResponse<WalletBalanceResponse> getBalance(@PathVariable("userId") Long userId) {
        long balance = walletService.getBalance(userId);
        return ApiResponse.ok(new WalletBalanceResponse(userId, balance));
    }

    @PostMapping("/wallets/{userId}/topup")
    public ApiResponse<WalletTxResponse> topup(
    		@PathVariable("userId") Long userId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody AmountRequest request
    ) {
        WalletTxResult result = walletService.topup(userId, request.amount(), idempotencyKey);
        return ApiResponse.ok(toResponse(result));
    }

    @PostMapping("/wallets/{userId}/pay")
    public ApiResponse<WalletTxResponse> pay(
    		@PathVariable("userId") Long userId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody AmountRequest request
    ) {
        WalletTxResult result = walletService.pay(userId, request.amount(), idempotencyKey);
        return ApiResponse.ok(toResponse(result));
    }

    private WalletTxResponse toResponse(WalletTxResult r) {
        return new WalletTxResponse(
                r.txId(),
                r.userId(),
                r.idempotencyKey(),
                r.type(),
                r.amount(),
                r.status(),
                r.balanceAfter(),
                r.createdAt()
        );
    }
}
