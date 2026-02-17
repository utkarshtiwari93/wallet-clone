package com.utkarsh.paytm_wallet_clone.repository;

import com.utkarsh.paytm_wallet_clone.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // All transactions where the wallet is either sender or receiver (paginated)
    @Query("SELECT t FROM Transaction t WHERE t.senderWallet.id = :walletId OR t.receiverWallet.id = :walletId ORDER BY t.createdAt DESC")
    Page<Transaction> findByWalletId(Long walletId, Pageable pageable);

    Optional<Transaction> findByTxnRef(String txnRef);
}