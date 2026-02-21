package com.utkarsh.paytm_wallet_clone.service;

import com.utkarsh.paytm_wallet_clone.dto.response.TransactionDTO;
import com.utkarsh.paytm_wallet_clone.exception.WalletNotFoundException;
import com.utkarsh.paytm_wallet_clone.model.Transaction;
import com.utkarsh.paytm_wallet_clone.model.User;
import com.utkarsh.paytm_wallet_clone.model.Wallet;
import com.utkarsh.paytm_wallet_clone.model.enums.TransactionStatus;
import com.utkarsh.paytm_wallet_clone.model.enums.TransactionType;
import com.utkarsh.paytm_wallet_clone.repository.TransactionRepository;
import com.utkarsh.paytm_wallet_clone.repository.WalletRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              WalletRepository walletRepository) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
    }

    // ─── Record a CREDIT transaction (deposit/top-up) ─────────────────────────

    @Transactional
    public Transaction recordCredit(Wallet wallet, BigDecimal amount, String description) {
        Transaction txn = new Transaction();
        txn.setReceiverWallet(wallet);
        txn.setSenderWallet(null);            // null sender = external deposit
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
        txn.setSenderWallet(wallet);
        txn.setReceiverWallet(null);          // null receiver = external withdrawal
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

    // ─── Get Transaction History (Paginated) ───────────────────────────────────

    @Transactional(readOnly = true)
    public Page<TransactionDTO> getTransactionHistory(User user, int page, int size) {
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new WalletNotFoundException(
                        "Wallet not found for user: " + user.getEmail()));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Transaction> transactions = transactionRepository.findByWalletId(wallet.getId(), pageable);

        return transactions.map(txn -> toDTO(txn, wallet));
    }

    // ─── Get Single Transaction by Reference ───────────────────────────────────

    @Transactional(readOnly = true)
    public Optional<TransactionDTO> getTransactionByRef(User user, String txnRef) {
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new WalletNotFoundException(
                        "Wallet not found for user: " + user.getEmail()));

        Optional<Transaction> txn = transactionRepository.findByTxnRef(txnRef);

        // Verify transaction belongs to this user
        if (txn.isPresent()) {
            Transaction t = txn.get();
            boolean belongsToUser = (t.getSenderWallet() != null && t.getSenderWallet().getId().equals(wallet.getId())) ||
                    (t.getReceiverWallet() != null && t.getReceiverWallet().getId().equals(wallet.getId()));
            if (belongsToUser) {
                return Optional.of(toDTO(t, wallet));
            }
        }
        return Optional.empty();
    }

    // ─── Helper: Convert Transaction to DTO with direction ─────────────────────

    private TransactionDTO toDTO(Transaction txn, Wallet userWallet) {
        TransactionDTO.TransactionDirection direction;
        String counterpartyName = null;
        String counterpartyPhone = null;

        // Determine direction and counterparty based on user's role
        if (txn.getType() == TransactionType.CREDIT) {
            direction = TransactionDTO.TransactionDirection.RECEIVED;
        } else if (txn.getType() == TransactionType.DEBIT) {
            direction = TransactionDTO.TransactionDirection.SENT;
        } else { // TRANSFER
            if (txn.getSenderWallet().getId().equals(userWallet.getId())) {
                direction = TransactionDTO.TransactionDirection.SENT;
                if (txn.getReceiverWallet() != null) {
                    counterpartyName = txn.getReceiverWallet().getUser().getName();
                    counterpartyPhone = txn.getReceiverWallet().getUser().getPhone();
                }
            } else {
                direction = TransactionDTO.TransactionDirection.RECEIVED;
                if (txn.getSenderWallet() != null) {
                    counterpartyName = txn.getSenderWallet().getUser().getName();
                    counterpartyPhone = txn.getSenderWallet().getUser().getPhone();
                }
            }
        }

        return new TransactionDTO(
                txn.getTxnRef(),
                direction,
                txn.getType().name(),
                txn.getAmount(),
                txn.getStatus().name(),
                txn.getDescription(),
                counterpartyName,
                counterpartyPhone,
                txn.getCreatedAt()
        );
    }
}