package com.utkarsh.paytm_wallet_clone.dto.response;

import java.math.BigDecimal;

public class PaymentOrderResponse {

    private String razorpayOrderId;  // e.g. "order_PxxxxXXXXXX"
    private BigDecimal amount;
    private String currency;
    private String receipt;
    private String status;
    private String keyId;            // returned so frontend can init Razorpay checkout

    public PaymentOrderResponse(String razorpayOrderId, BigDecimal amount,
                                String currency, String receipt,
                                String status, String keyId) {
        this.razorpayOrderId = razorpayOrderId;
        this.amount = amount;
        this.currency = currency;
        this.receipt = receipt;
        this.status = status;
        this.keyId = keyId;
    }

    public String getRazorpayOrderId() { return razorpayOrderId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getReceipt() { return receipt; }
    public String getStatus() { return status; }
    public String getKeyId() { return keyId; }
}