package com.utkarsh.paytm_wallet_clone.service;

import com.utkarsh.paytm_wallet_clone.dto.response.UserLookupDTO;
import com.utkarsh.paytm_wallet_clone.dto.response.WalletBalanceDTO;
import com.utkarsh.paytm_wallet_clone.exception.WalletNotFoundException;
import com.utkarsh.paytm_wallet_clone.model.User;
import com.utkarsh.paytm_wallet_clone.model.Wallet;
import com.utkarsh.paytm_wallet_clone.repository.UserRepository;
import com.utkarsh.paytm_wallet_clone.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    public WalletService(WalletRepository walletRepository, UserRepository userRepository) {
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
    }

    // ─── Get balance for the logged-in user ──────────────────────────────────

    @Transactional(readOnly = true)
    public WalletBalanceDTO getBalance(User user) {
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new WalletNotFoundException(
                        "Wallet not found for user: " + user.getEmail()));

        return new WalletBalanceDTO(
                wallet.getId(),
                wallet.getBalance(),
                wallet.getCurrency(),
                user.getId(),
                user.getName()
        );
    }

    // ─── Lookup user by phone number ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public UserLookupDTO lookupUserByPhone(String phone) {
        return userRepository.findByPhone(phone)
                .map(user -> new UserLookupDTO(user.getName(), user.getPhone(), true))
                .orElse(UserLookupDTO.notFound());
    }

    // ─── Internal helper used by TransferService, PaymentService etc. ─────────

    @Transactional(readOnly = true)
    public Wallet getWalletByUser(User user) {
        return walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new WalletNotFoundException(
                        "Wallet not found for user: " + user.getEmail()));
    }

    // ─── Credit wallet (used by payment webhook) ──────────────────────────────

    @Transactional
    public void creditWallet(Wallet wallet, BigDecimal amount) {
        BigDecimal newBalance = wallet.getBalance().add(amount);
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);
        System.out.println("✅ Wallet " + wallet.getId() + " credited ₹" + amount + " → new balance: ₹" + newBalance);
    }

    // ─── Debit wallet (used by transfers/withdrawals) ─────────────────────────

    @Transactional
    public void debitWallet(Wallet wallet, BigDecimal amount) {
        BigDecimal newBalance = wallet.getBalance().subtract(amount);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Insufficient funds");
        }
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);
        System.out.println("✅ Wallet " + wallet.getId() + " debited ₹" + amount + " → new balance: ₹" + newBalance);
    }
}