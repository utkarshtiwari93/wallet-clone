package com.utkarsh.paytm_wallet_clone.dto.response;

import java.math.BigDecimal;

public class TransferResponse {

    private String txnRef;          // UUID reference for this transfer
    private String senderName;
    private String recipientName;
    private String recipientPhone;
    private BigDecimal amount;
    private String status;
    private String note;
    private BigDecimal newBalance;  // sender's new balance after transfer

    public TransferResponse(String txnRef, String senderName, String recipientName,
                            String recipientPhone, BigDecimal amount, String status,
                            String note, BigDecimal newBalance) {
        this.txnRef = txnRef;
        this.senderName = senderName;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.amount = amount;
        this.status = status;
        this.note = note;
        this.newBalance = newBalance;
    }

    // Getters
    public String getTxnRef() { return txnRef; }
    public String getSenderName() { return senderName; }
    public String getRecipientName() { return recipientName; }
    public String getRecipientPhone() { return recipientPhone; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public String getNote() { return note; }
    public BigDecimal getNewBalance() { return newBalance; }
}