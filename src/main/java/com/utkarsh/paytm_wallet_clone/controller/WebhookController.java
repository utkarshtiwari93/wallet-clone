package com.utkarsh.paytm_wallet_clone.controller;

import com.utkarsh.paytm_wallet_clone.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private final PaymentService paymentService;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    public WebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // POST /webhook/razorpay
    // Called by Razorpay when payment events occur
    @PostMapping("/razorpay")
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestBody String rawBody,
            @RequestHeader("X-Razorpay-Signature") String receivedSignature) {

        // ─── CRITICAL: Verify signature FIRST ────────────────────────────────
        if (!verifySignature(rawBody, receivedSignature)) {
            System.out.println("❌ WEBHOOK REJECTED: Invalid signature");
            return ResponseEntity.status(400).body("Invalid signature");
        }

        System.out.println("✅ Webhook signature verified");

        // ─── Parse the event ──────────────────────────────────────────────────
        try {
            // Razorpay sends JSON, we need to extract event type and payload
            org.json.JSONObject json = new org.json.JSONObject(rawBody);
            String event = json.getString("event");
            org.json.JSONObject payload = json.getJSONObject("payload")
                    .getJSONObject("payment")
                    .getJSONObject("entity");

            System.out.println("📦 Webhook event: " + event);

            if ("payment.captured".equals(event)) {
                String paymentId = payload.getString("id");
                String orderId = payload.getString("order_id");
                int amountInPaise = payload.getInt("amount");

                paymentService.handlePaymentSuccess(orderId, paymentId, amountInPaise);
            }

            return ResponseEntity.ok("Webhook processed");

        } catch (Exception e) {
            System.err.println("❌ Webhook processing error: " + e.getMessage());
            return ResponseEntity.status(500).body("Processing failed");
        }
    }

    // ─── HMAC-SHA256 signature verification ──────────────────────────────────
    private boolean verifySignature(String rawBody, String receivedSignature) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);

            byte[] hash = sha256Hmac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            String computedSignature = hexString.toString();
            return computedSignature.equals(receivedSignature);

        } catch (Exception e) {
            System.err.println("❌ Signature verification failed: " + e.getMessage());
            return false;
        }
    }
}