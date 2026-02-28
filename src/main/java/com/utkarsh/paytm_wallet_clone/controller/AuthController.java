package com.utkarsh.paytm_wallet_clone.controller;

import com.utkarsh.paytm_wallet_clone.dto.request.ChangePasswordRequest;
import com.utkarsh.paytm_wallet_clone.dto.request.ForgotPasswordRequest;
import com.utkarsh.paytm_wallet_clone.dto.request.LoginRequest;
import com.utkarsh.paytm_wallet_clone.dto.request.RegisterRequest;
import com.utkarsh.paytm_wallet_clone.dto.request.ResetPasswordRequest;
import com.utkarsh.paytm_wallet_clone.dto.response.AuthResponse;
import com.utkarsh.paytm_wallet_clone.model.User;
import com.utkarsh.paytm_wallet_clone.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // ─── Profile ──────────────────────────────────────────────────────────────

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of(
                "name", user.getName(),
                "email", user.getEmail(),
                "phone", user.getPhone() != null ? user.getPhone() : "",
                "memberSince", user.getCreatedAt().toString()));
    }

    // ─── Change Password ──────────────────────────────────────────────────────

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal User user) {

        authService.changePassword(user, request.getCurrentPassword(), request.getNewPassword());

        return ResponseEntity.ok(Map.of(
                "message", "Password changed successfully"));
    }

    // ─── Forgot Password ───────────────────────────────────────────────────────

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        String token = authService.forgotPassword(request);

        // In production, send this token via email instead of returning it
        // For now, return it for testing purposes
        return ResponseEntity.ok(Map.of(
                "message", "Password reset token generated",
                "token", token // Remove this in production!
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(request);

        return ResponseEntity.ok(Map.of(
                "message", "Password reset successful"));
    }
}
