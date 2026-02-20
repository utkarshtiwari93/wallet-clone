package com.utkarsh.paytm_wallet_clone.model;

import com.utkarsh.paytm_wallet_clone.model.enums.TransactionStatus;
import com.utkarsh.paytm_wallet_clone.model.enums.TransactionType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique business-level reference for this transaction
    @Column(nullable = false, unique = true, updatable = false)
    private String txnRef = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_wallet_id")
    private Wallet senderWallet;   // null for CREDIT (top-up)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_wallet_id")
    private Wallet receiverWallet; // null for DEBIT (withdrawal)

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;   // CREDIT | DEBIT | TRANSFER

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status = TransactionStatus.PENDING; // PENDING | SUCCESS | FAILED

    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ─── Enums ───────────────────────────────────────────────────────────────

// All the code are inside enum package .....


    // ─── Getters & Setters ───────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTxnRef() { return txnRef; }
    public void setTxnRef(String txnRef) { this.txnRef = txnRef; }

    public Wallet getSenderWallet() { return senderWallet; }
    public void setSenderWallet(Wallet senderWallet) { this.senderWallet = senderWallet; }

    public Wallet getReceiverWallet() { return receiverWallet; }
    public void setReceiverWallet(Wallet receiverWallet) { this.receiverWallet = receiverWallet; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}