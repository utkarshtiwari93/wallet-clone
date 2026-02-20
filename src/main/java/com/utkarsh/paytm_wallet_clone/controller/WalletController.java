package com.utkarsh.paytm_wallet_clone.controller;

import com.utkarsh.paytm_wallet_clone.dto.response.WalletBalanceDTO;
import com.utkarsh.paytm_wallet_clone.model.User;
import com.utkarsh.paytm_wallet_clone.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    // GET /api/wallet/balance
    // Requires: Authorization: Bearer <token>
    @GetMapping("/balance")
    public ResponseEntity<WalletBalanceDTO> getBalance(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(walletService.getBalance(user));
    }
}