package com.utkarsh.paytm_wallet_clone.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "razorpay_orders")
public class RazorpayOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The order ID returned by Razorpay API (e.g. "order_Pxyz1234")
    @Column(name = "order_id", nullable = false, unique = true)
    private String razorpayOrderId;

    // The payment ID returned after successful payment
    @Column(name = "payment_id", unique = true)
    private String razorpayPaymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Amount in INR (Razorpay internally works in paise, but we store in rupees)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RazorpayOrderStatus status = RazorpayOrderStatus.CREATED;

    @Column(length = 150)
    private String receipt; // your internal reference e.g. "receipt_user1_20240217"

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime paidAt;

    // ─── Enum ────────────────────────────────────────────────────────────────

    public enum RazorpayOrderStatus {
        CREATED, PAID, FAILED, REFUNDED
    }

    // ─── Getters & Setters ───────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }

    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public RazorpayOrderStatus getStatus() { return status; }
    public void setStatus(RazorpayOrderStatus status) { this.status = status; }

    public String getReceipt() { return receipt; }
    public void setReceipt(String receipt) { this.receipt = receipt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
}