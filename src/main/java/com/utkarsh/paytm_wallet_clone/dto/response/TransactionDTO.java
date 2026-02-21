package com.utkarsh.paytm_wallet_clone.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionDTO {

    private String txnRef;
    private TransactionDirection direction;  // SENT | RECEIVED
    private String type;                      // CREDIT | DEBIT | TRANSFER
    private BigDecimal amount;
    private String status;
    private String description;
    private String counterpartyName;          // other person's name (for transfers)
    private String counterpartyPhone;         // other person's phone
    private LocalDateTime createdAt;

    public enum TransactionDirection {
        SENT,      // User is the sender (money out)
        RECEIVED   // User is the receiver (money in)
    }

    public TransactionDTO(String txnRef, TransactionDirection direction, String type,
                          BigDecimal amount, String status, String description,
                          String counterpartyName, String counterpartyPhone,
                          LocalDateTime createdAt) {
        this.txnRef = txnRef;
        this.direction = direction;
        this.type = type;
        this.amount = amount;
        this.status = status;
        this.description = description;
        this.counterpartyName = counterpartyName;
        this.counterpartyPhone = counterpartyPhone;
        this.createdAt = createdAt;
    }

    // Getters
    public String getTxnRef() { return txnRef; }
    public TransactionDirection getDirection() { return direction; }
    public String getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public String getDescription() { return description; }
    public String getCounterpartyName() { return counterpartyName; }
    public String getCounterpartyPhone() { return counterpartyPhone; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}