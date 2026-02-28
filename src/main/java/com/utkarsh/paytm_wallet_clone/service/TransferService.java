package com.utkarsh.paytm_wallet_clone.service;

import com.utkarsh.paytm_wallet_clone.dto.request.TransferRequest;
import com.utkarsh.paytm_wallet_clone.dto.response.TransferResponse;
import com.utkarsh.paytm_wallet_clone.exception.InsufficientFundsException;
import com.utkarsh.paytm_wallet_clone.exception.UserNotFoundException;
import com.utkarsh.paytm_wallet_clone.exception.WalletNotFoundException;
import com.utkarsh.paytm_wallet_clone.model.Transaction;
import com.utkarsh.paytm_wallet_clone.model.User;
import com.utkarsh.paytm_wallet_clone.model.Wallet;
import com.utkarsh.paytm_wallet_clone.repository.UserRepository;
import com.utkarsh.paytm_wallet_clone.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionService transactionService;
    private final WebSocketService webSocketService;  // ← ADD THIS

    public TransferService(UserRepository userRepository,
                           WalletRepository walletRepository,
                           TransactionService transactionService,
                           WebSocketService webSocketService) {  // ← ADD THIS
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionService = transactionService;
        this.webSocketService = webSocketService;  // ← ADD THIS
    }

    @Transactional
    public TransferResponse transfer(User sender, TransferRequest request) {

        BigDecimal amount = request.getAmount();
        String recipientPhone = request.getRecipientPhone();

        log.info("Transfer initiated: {} → {} | Amount: ₹{}",
                sender.getEmail(), recipientPhone, amount);

        // 1. Validate not self-transfer
        if (sender.getPhone().equals(recipientPhone)) {
            log.warn("Self-transfer blocked: {}", sender.getEmail());
            throw new IllegalArgumentException("Cannot transfer to yourself");
        }

        // 2. Find recipient
        User recipient = userRepository.findByPhone(recipientPhone)
                .orElseThrow(() -> {
                    log.warn("Recipient not found: {}", recipientPhone);
                    return new UserNotFoundException("Recipient not found with phone: " + recipientPhone);
                });

        // 3. Lock wallets in consistent order
        Wallet senderWallet;
        Wallet recipientWallet;

        if (sender.getId() < recipient.getId()) {
            senderWallet = lockWallet(sender.getId());
            recipientWallet = lockWallet(recipient.getId());
        } else {
            recipientWallet = lockWallet(recipient.getId());
            senderWallet = lockWallet(sender.getId());
        }

        // 4. Check funds
        if (senderWallet.getBalance().compareTo(amount) < 0) {
            log.warn("Insufficient funds: {} | Available: ₹{} | Required: ₹{}",
                    sender.getEmail(), senderWallet.getBalance(), amount);
            throw new InsufficientFundsException(
                    "Insufficient funds. Available: ₹" + senderWallet.getBalance());
        }

        // 5. Debit sender
        BigDecimal newSenderBalance = senderWallet.getBalance().subtract(amount);
        senderWallet.setBalance(newSenderBalance);
        walletRepository.save(senderWallet);

        // 6. Credit recipient
        BigDecimal newRecipientBalance = recipientWallet.getBalance().add(amount);
        recipientWallet.setBalance(newRecipientBalance);
        walletRepository.save(recipientWallet);

        // 7. Record transaction
        String description = request.getNote() != null ?
                "Transfer: " + request.getNote() :
                "Transfer to " + recipient.getName();

        Transaction txn = transactionService.recordTransfer(
                senderWallet, recipientWallet, amount, description);

        log.info("✅ Transfer completed: {} → {} | Amount: ₹{} | TxnRef: {}",
                sender.getEmail(), recipient.getEmail(), amount, txn.getTxnRef());

        // ========== 8. SEND WEBSOCKET NOTIFICATIONS ==========

        try {
            // Notify sender
            webSocketService.notifyTransferSent(
                    sender.getEmail(),
                    recipient.getName(),
                    amount,
                    newSenderBalance
            );
            log.info("🔔 Sent WebSocket to sender: {}", sender.getEmail());

            // Notify recipient
            webSocketService.notifyTransferReceived(
                    recipient.getEmail(),
                    sender.getName(),
                    amount,
                    newRecipientBalance
            );
            log.info("🔔 Sent WebSocket to recipient: {}", recipient.getEmail());

        } catch (Exception e) {
            log.error("❌ Failed to send WebSocket notification", e);
            // Don't fail the transfer if notification fails
        }

        return new TransferResponse(
                txn.getTxnRef(),
                sender.getName(),
                recipient.getName(),
                recipientPhone,
                amount,
                "SUCCESS",
                request.getNote(),
                newSenderBalance
        );
    }

    private Wallet lockWallet(Long userId) {
        return walletRepository.findByIdForUpdate(
                walletRepository.findByUserId(userId)
                        .orElseThrow(() -> new WalletNotFoundException("Wallet not found"))
                        .getId()
        ).orElseThrow(() -> new WalletNotFoundException("Wallet lock failed"));
    }
}