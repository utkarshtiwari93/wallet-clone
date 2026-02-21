package com.utkarsh.paytm_wallet_clone.controller;

import com.utkarsh.paytm_wallet_clone.dto.request.TransferRequest;
import com.utkarsh.paytm_wallet_clone.dto.response.TransactionDTO;
import com.utkarsh.paytm_wallet_clone.dto.response.TransferResponse;
import com.utkarsh.paytm_wallet_clone.dto.response.UserLookupDTO;
import com.utkarsh.paytm_wallet_clone.dto.response.WalletBalanceDTO;
import com.utkarsh.paytm_wallet_clone.model.User;
import com.utkarsh.paytm_wallet_clone.service.TransactionService;
import com.utkarsh.paytm_wallet_clone.service.TransferService;
import com.utkarsh.paytm_wallet_clone.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;
    private final TransferService transferService;
    private final TransactionService transactionService;

    public WalletController(WalletService walletService,
                            TransferService transferService,
                            TransactionService transactionService) {
        this.walletService = walletService;
        this.transferService = transferService;
        this.transactionService = transactionService;
    }

    // GET /api/wallet/balance
    @GetMapping("/balance")
    public ResponseEntity<WalletBalanceDTO> getBalance(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(walletService.getBalance(user));
    }

    // GET /api/wallet/user/{phone} - NEW: Lookup user by phone
    @GetMapping("/user/{phone}")
    public ResponseEntity<UserLookupDTO> lookupUserByPhone(
            @PathVariable String phone,
            @AuthenticationPrincipal User currentUser) {

        UserLookupDTO lookup = walletService.lookupUserByPhone(phone);
        return ResponseEntity.ok(lookup);
    }

    // POST /api/wallet/transfer
    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(transferService.transfer(user, request));
    }

    // GET /api/wallet/transactions?page=0&size=10
    @GetMapping("/transactions")
    public ResponseEntity<Page<TransactionDTO>> getTransactionHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User user) {

        Page<TransactionDTO> history = transactionService.getTransactionHistory(user, page, size);
        return ResponseEntity.ok(history);
    }

    // GET /api/wallet/transactions/{txnRef}
    @GetMapping("/transactions/{txnRef}")
    public ResponseEntity<TransactionDTO> getTransactionByRef(
            @PathVariable String txnRef,
            @AuthenticationPrincipal User user) {

        return transactionService.getTransactionByRef(user, txnRef)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}