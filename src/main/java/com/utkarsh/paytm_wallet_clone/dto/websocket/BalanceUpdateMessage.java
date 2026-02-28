package com.utkarsh.paytm_wallet_clone.dto.websocket;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BalanceUpdateMessage {
    
    private String type;              // "CREDIT" | "DEBIT" | "TRANSFER_RECEIVED"
    private BigDecimal newBalance;
    private BigDecimal amount;
    private String fromUser;          // Sender name (for transfers)
    private String message;           // Notification message
    private LocalDateTime timestamp;
    
    public BalanceUpdateMessage(String type, BigDecimal newBalance, BigDecimal amount, 
                                 String fromUser, String message) {
        this.type = type;
        this.newBalance = newBalance;
        this.amount = amount;
        this.fromUser = fromUser;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters & Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public BigDecimal getNewBalance() { return newBalance; }
    public void setNewBalance(BigDecimal newBalance) { this.newBalance = newBalance; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getFromUser() { return fromUser; }
    public void setFromUser(String fromUser) { this.fromUser = fromUser; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
