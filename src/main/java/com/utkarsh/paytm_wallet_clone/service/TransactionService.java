package com.utkarsh.paytm_wallet_clone.service;

import com.utkarsh.paytm_wallet_clone.model.Transaction;
import com.utkarsh.paytm_wallet_clone.model.Wallet;
import com.utkarsh.paytm_wallet_clone.model.enums.TransactionStatus;
import com.utkarsh.paytm_wallet_clone.model.enums.TransactionType;
import com.utkarsh.paytm_wallet_clone.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    // ─── Record a CREDIT transaction (deposit/top-up) ─────────────────────────

    @Transactional
    public Transaction recordCredit(Wallet wallet, BigDecimal amount, String description) {
        Transaction txn = new Transaction();
        txn.setReceiverWallet(wallet);        // null sender = external deposit
        txn.setSenderWallet(null);
        txn.setAmount(amount);
        txn.setType(TransactionType.CREDIT);
        txn.setStatus(TransactionStatus.SUCCESS);
        txn.setDescription(description);
        return transactionRepository.save(txn);
    }

    // ─── Record a DEBIT transaction (withdrawal) ───────────────────────────────

    @Transactional
    public Transaction recordDebit(Wallet wallet, BigDecimal amount, String description) {
        Transaction txn = new Transaction();
        txn.setSenderWallet(wallet);          // null receiver = external withdrawal
        txn.setReceiverWallet(null);
        txn.setAmount(amount);
        txn.setType(TransactionType.DEBIT);
        txn.setStatus(TransactionStatus.SUCCESS);
        txn.setDescription(description);
        return transactionRepository.save(txn);
    }

    // ─── Record a TRANSFER transaction (wallet-to-wallet) ─────────────────────

    @Transactional
    public Transaction recordTransfer(Wallet sender, Wallet receiver,
                                      BigDecimal amount, String description) {
        Transaction txn = new Transaction();
        txn.setSenderWallet(sender);
        txn.setReceiverWallet(receiver);
        txn.setAmount(amount);
        txn.setType(TransactionType.TRANSFER);
        txn.setStatus(TransactionStatus.SUCCESS);
        txn.setDescription(description);
        return transactionRepository.save(txn);
    }
}