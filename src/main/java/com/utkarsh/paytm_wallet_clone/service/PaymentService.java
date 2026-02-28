package com.utkarsh.paytm_wallet_clone.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.utkarsh.paytm_wallet_clone.dto.request.CreateOrderRequest;
import com.utkarsh.paytm_wallet_clone.dto.response.PaymentOrderResponse;
import com.utkarsh.paytm_wallet_clone.model.RazorpayOrder;
import com.utkarsh.paytm_wallet_clone.model.User;
import com.utkarsh.paytm_wallet_clone.model.Wallet;
import com.utkarsh.paytm_wallet_clone.repository.RazorpayOrderRepository;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final RazorpayClient razorpayClient;
    private final RazorpayOrderRepository razorpayOrderRepository;
    private final WalletService walletService;
    private final TransactionService transactionService;
    private final WebSocketService webSocketService;  // ← ADD THIS

    @Value("${razorpay.key-id}")
    private String keyId;

    public PaymentService(RazorpayClient razorpayClient,
                          RazorpayOrderRepository razorpayOrderRepository,
                          WalletService walletService,
                          TransactionService transactionService,
                          WebSocketService webSocketService) {  // ← ADD THIS
        this.razorpayClient = razorpayClient;
        this.razorpayOrderRepository = razorpayOrderRepository;
        this.walletService = walletService;
        this.transactionService = transactionService;
        this.webSocketService = webSocketService;  // ← ADD THIS
    }

    @Transactional
    public PaymentOrderResponse createOrder(CreateOrderRequest request, User user)
            throws RazorpayException {

        BigDecimal amount = request.getAmount();
        int amountInPaise = amount.multiply(BigDecimal.valueOf(100)).intValue();
        String receipt = "receipt_" + user.getId() + "_" + System.currentTimeMillis();

        log.info("Creating Razorpay order for user {} | Amount: ₹{}", user.getEmail(), amount);

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", receipt);
        orderRequest.put("payment_capture", 1);

        Order razorpayOrder = razorpayClient.orders.create(orderRequest);
        String razorpayOrderId = razorpayOrder.get("id");

        RazorpayOrder dbOrder = new RazorpayOrder();
        dbOrder.setRazorpayOrderId(razorpayOrderId);
        dbOrder.setUser(user);
        dbOrder.setAmount(amount);
        dbOrder.setCurrency("INR");
        dbOrder.setReceipt(receipt);
        dbOrder.setStatus(RazorpayOrder.RazorpayOrderStatus.CREATED);
        razorpayOrderRepository.save(dbOrder);

        log.info("Razorpay order created: {} | User: {}", razorpayOrderId, user.getEmail());

        return new PaymentOrderResponse(
                razorpayOrderId, amount, "INR", receipt, "CREATED", keyId);
    }

    @Transactional
    public void handlePaymentSuccess(String razorpayOrderId, String paymentId, int amountInPaise) {

        log.info("🔔 Webhook received: Payment {} for order {}", paymentId, razorpayOrderId);

        RazorpayOrder order = razorpayOrderRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getRazorpayPaymentId() != null) {
            log.warn("⚠️ Payment already processed: {}", paymentId);
            return;
        }

        order.setRazorpayPaymentId(paymentId);
        order.setStatus(RazorpayOrder.RazorpayOrderStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        razorpayOrderRepository.save(order);

        BigDecimal amountInRupees = BigDecimal.valueOf(amountInPaise).divide(BigDecimal.valueOf(100));
        User user = order.getUser();
        Wallet wallet = walletService.getWalletByUser(user);

        BigDecimal oldBalance = wallet.getBalance();
        walletService.creditWallet(wallet, amountInRupees);
        BigDecimal newBalance = wallet.getBalance();

        log.info("💰 Wallet credited: User {} | Amount: ₹{} | New balance: ₹{}",
                user.getEmail(), amountInRupees, newBalance);

        transactionService.recordCredit(wallet, amountInRupees, "Razorpay payment: " + paymentId);

        // ========== SEND WEBSOCKET NOTIFICATION ==========

        try {
            webSocketService.notifyPaymentReceived(
                    user.getEmail(),
                    amountInRupees,
                    newBalance
            );
            log.info("🔔 Sent WebSocket to user: {}", user.getEmail());
        } catch (Exception e) {
            log.error("❌ Failed to send WebSocket notification", e);
        }

        log.info("✅ Payment processed: {} | User: {}", paymentId, user.getEmail());
    }
}