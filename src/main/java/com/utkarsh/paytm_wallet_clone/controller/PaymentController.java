package com.utkarsh.paytm_wallet_clone.controller;

import com.razorpay.RazorpayException;
import com.utkarsh.paytm_wallet_clone.dto.request.CreateOrderRequest;
import com.utkarsh.paytm_wallet_clone.dto.response.PaymentOrderResponse;
import com.utkarsh.paytm_wallet_clone.model.User;
import com.utkarsh.paytm_wallet_clone.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // POST /api/payment/create-order
    // Body: { "amount": 500.00 }
    // Requires: Authorization: Bearer <token>
    @PostMapping("/create-order")
    public ResponseEntity<PaymentOrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal User user) throws RazorpayException {

        PaymentOrderResponse response = paymentService.createOrder(request, user);
        return ResponseEntity.ok(response);
    }
}