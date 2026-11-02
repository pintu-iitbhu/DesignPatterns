package com.rcpky.controller;

import com.rcpky.dto.AmountRequest;
import com.rcpky.dto.ApiResponse;
import com.rcpky.dto.CreateWalletRequest;
import com.rcpky.dto.TransferRequest;
import com.rcpky.model.Transaction;
import com.rcpky.model.Wallet;
import com.rcpky.service.IWalletService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    private final IWalletService walletService;

    public WalletController(IWalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Wallet>> createWallet(@RequestBody CreateWalletRequest req) {
        Wallet wallet = walletService.createWallet(req.getOwnerName(), req.getCurrency(),
                req.getInitialBalance() != null ? req.getInitialBalance() : java.math.BigDecimal.ZERO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(wallet));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Wallet>>> getAllWallets() {
        return ResponseEntity.ok(ApiResponse.ok(walletService.getAllWallets()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Wallet>> getWallet(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(walletService.getWallet(id)));
    }

    @PostMapping("/{id}/credit")
    public ResponseEntity<ApiResponse<Transaction>> credit(
            @PathVariable String id,
            @RequestBody AmountRequest req,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        Transaction t = walletService.credit(id, req.getAmount(), req.getDescription(), idempotencyKey);
        return ResponseEntity.ok(ApiResponse.ok(t));
    }

    @PostMapping("/{id}/debit")
    public ResponseEntity<ApiResponse<Transaction>> debit(
            @PathVariable String id,
            @RequestBody AmountRequest req,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        Transaction t = walletService.debit(id, req.getAmount(), req.getDescription(), idempotencyKey);
        return ResponseEntity.ok(ApiResponse.ok(t));
    }

    @PostMapping("/{id}/transfer")
    public ResponseEntity<ApiResponse<List<Transaction>>> transfer(
            @PathVariable String id,
            @RequestBody TransferRequest req,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        List<Transaction> txns = walletService.transfer(id, req.getToWalletId(),
                req.getAmount(), req.getDescription(), idempotencyKey);
        return ResponseEntity.ok(ApiResponse.ok(txns));
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<ApiResponse<List<Transaction>>> getTransactions(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(walletService.getTransactionHistory(id)));
    }
}
