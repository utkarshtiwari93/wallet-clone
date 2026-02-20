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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class PaymentService {

    private final RazorpayClient razorpayClient;
    private final RazorpayOrderRepository razorpayOrderRepository;
    private final WalletService walletService;
    private final TransactionService transactionService;

    @Value("${razorpay.key-id}")
    private String keyId;

    public PaymentService(RazorpayClient razorpayClient,
                          RazorpayOrderRepository razorpayOrderRepository,
                          WalletService walletService,
                          TransactionService transactionService) {
        this.razorpayClient = razorpayClient;
        this.razorpayOrderRepository = razorpayOrderRepository;
        this.walletService = walletService;
        this.transactionService = transactionService;
    }

    // ─── Create Razorpay Order ────────────────────────────────────────────────

    @Transactional
    public PaymentOrderResponse createOrder(CreateOrderRequest request, User user)
            throws RazorpayException {

        BigDecimal amount = request.getAmount();
        int amountInPaise = amount.multiply(BigDecimal.valueOf(100)).intValue();
        String receipt = "receipt_" + user.getId() + "_" + System.currentTimeMillis();

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
        dbOrder.setStatus(RazorpayOrder.RazorpayOrderStatus.CREATED);  // ← Fixed
        razorpayOrderRepository.save(dbOrder);

        return new PaymentOrderResponse(
                razorpayOrderId, amount, "INR", receipt, "CREATED", keyId);
    }

    // ─── Handle Payment Success (called by webhook) ───────────────────────────

    @Transactional
    public void handlePaymentSuccess(String razorpayOrderId, String paymentId, int amountInPaise) {

        System.out.println("💰 Processing payment: " + paymentId + " for order: " + razorpayOrderId);

        // 1. Find the order in DB
        RazorpayOrder order = razorpayOrderRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + razorpayOrderId));

        // 2. IDEMPOTENCY CHECK: if already processed, skip
        if (order.getRazorpayPaymentId() != null) {
            System.out.println("⚠️ Payment already processed, skipping: " + paymentId);
            return;
        }

        // 3. Update order status to PAID
        order.setRazorpayPaymentId(paymentId);
        order.setStatus(RazorpayOrder.RazorpayOrderStatus.PAID);  // ← Fixed
        order.setPaidAt(LocalDateTime.now());
        razorpayOrderRepository.save(order);

        // 4. Credit the wallet
        BigDecimal amountInRupees = BigDecimal.valueOf(amountInPaise).divide(BigDecimal.valueOf(100));
        User user = order.getUser();
        Wallet wallet = walletService.getWalletByUser(user);
        walletService.creditWallet(wallet, amountInRupees);

        // 5. Record CREDIT transaction in ledger
        transactionService.recordCredit(
                wallet,
                amountInRupees,
                "Razorpay payment: " + paymentId
        );

        System.out.println("✅ Payment processed successfully");
    }
}