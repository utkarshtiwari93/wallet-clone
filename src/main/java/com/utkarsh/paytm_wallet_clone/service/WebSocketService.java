package com.utkarsh.paytm_wallet_clone.service;

import com.utkarsh.paytm_wallet_clone.dto.websocket.BalanceUpdateMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class WebSocketService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketService.class);
    
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Send balance update to specific user
     */
    public void sendBalanceUpdate(String userEmail, String type, BigDecimal newBalance, 
                                    BigDecimal amount, String fromUser, String message) {
        
        BalanceUpdateMessage notification = new BalanceUpdateMessage(
                type, newBalance, amount, fromUser, message
        );
        
        // Send to user-specific queue
        messagingTemplate.convertAndSendToUser(
                userEmail,
                "/queue/balance",
                notification
        );
        
        log.info("🔔 WebSocket notification sent to {} | Type: {} | Amount: ₹{}", 
                userEmail, type, amount);
    }

    /**
     * Notify user of payment received via Razorpay
     */
    public void notifyPaymentReceived(String userEmail, BigDecimal amount, BigDecimal newBalance) {
        String message = String.format("💰 Payment of ₹%.2f received!", amount);
        sendBalanceUpdate(userEmail, "CREDIT", newBalance, amount, null, message);
    }

    /**
     * Notify user of transfer received from another user
     */
    public void notifyTransferReceived(String recipientEmail, String senderName, 
                                        BigDecimal amount, BigDecimal newBalance) {
        String message = String.format("💰 You received ₹%.2f from %s", amount, senderName);
        sendBalanceUpdate(recipientEmail, "TRANSFER_RECEIVED", newBalance, amount, senderName, message);
    }

    /**
     * Notify sender of transfer completed
     */
    public void notifyTransferSent(String senderEmail, String recipientName, 
                                     BigDecimal amount, BigDecimal newBalance) {
        String message = String.format("✅ Transfer of ₹%.2f to %s completed", amount, recipientName);
        sendBalanceUpdate(senderEmail, "DEBIT", newBalance, amount, recipientName, message);
    }
}
