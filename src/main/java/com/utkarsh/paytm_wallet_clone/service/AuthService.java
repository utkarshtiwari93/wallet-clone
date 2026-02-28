package com.utkarsh.paytm_wallet_clone.service;

import com.utkarsh.paytm_wallet_clone.dto.request.ForgotPasswordRequest;
import com.utkarsh.paytm_wallet_clone.dto.request.LoginRequest;
import com.utkarsh.paytm_wallet_clone.dto.request.RegisterRequest;
import com.utkarsh.paytm_wallet_clone.dto.request.ResetPasswordRequest;
import com.utkarsh.paytm_wallet_clone.dto.response.AuthResponse;
import com.utkarsh.paytm_wallet_clone.exception.DuplicateUserException;
import com.utkarsh.paytm_wallet_clone.exception.UserNotFoundException;
import com.utkarsh.paytm_wallet_clone.model.PasswordResetToken;
import com.utkarsh.paytm_wallet_clone.model.User;
import com.utkarsh.paytm_wallet_clone.model.Wallet;
import com.utkarsh.paytm_wallet_clone.repository.PasswordResetTokenRepository;
import com.utkarsh.paytm_wallet_clone.repository.UserRepository;
import com.utkarsh.paytm_wallet_clone.repository.WalletRepository;
import com.utkarsh.paytm_wallet_clone.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
            WalletRepository walletRepository,
            PasswordResetTokenRepository resetTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {

        log.info("Registration attempt for email: {}", req.getEmail());

        if (userRepository.existsByEmail(req.getEmail())) {
            log.warn("Registration failed: Email already exists - {}", req.getEmail());
            throw new DuplicateUserException("Email already registered: " + req.getEmail());
        }
        if (userRepository.existsByPhone(req.getPhone())) {
            log.warn("Registration failed: Phone already exists - {}", req.getPhone());
            throw new DuplicateUserException("Phone already registered: " + req.getPhone());
        }

        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setIsActive(true);
        userRepository.save(user);

        log.info("User registered successfully: {} (ID: {})", user.getEmail(), user.getId());

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        walletRepository.save(wallet);

        log.info("Wallet created for user: {} (Wallet ID: {})", user.getEmail(), wallet.getId());

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        return new AuthResponse(token, user.getEmail(), user.getId(), user.getName());
    }

    public AuthResponse login(LoginRequest req) {

        log.info("Login attempt for email: {}", req.getEmail());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        } catch (AuthenticationException e) {
            log.warn("Login failed for email: {} - Invalid credentials", req.getEmail());
            throw new UserNotFoundException("Invalid email or password");
        }

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        log.info("Login successful for user: {} (ID: {})", user.getEmail(), user.getId());

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        return new AuthResponse(token, user.getEmail(), user.getId(), user.getName());
    }

    // ─── Forgot Password: Generate Reset Token ────────────────────────────────

    @Transactional
    public String forgotPassword(ForgotPasswordRequest req) {

        log.info("Forgot password request for email: {}", req.getEmail());

        // Verify both email and phone match
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> {
                    log.warn("Forgot password failed: Email not found - {}", req.getEmail());
                    return new UserNotFoundException("Email or phone number not found");
                });

        if (!user.getPhone().equals(req.getPhone())) {
            log.warn("Forgot password failed: Phone mismatch for email - {}", req.getEmail());
            throw new UserNotFoundException("Email or phone number not found");
        }

        // Delete any existing tokens for this user
        resetTokenRepository.deleteByUserId(user.getId());

        // Generate new token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(user, token);
        resetTokenRepository.save(resetToken);

        log.info("Password reset token generated for user: {}", user.getEmail());

        // 🔥 Print token prominently in console (FOR TESTING ONLY — remove in
        // production)
        log.info("\n" +
                "╔══════════════════════════════════════════════════════════════════╗\n" +
                "║  🔑🔑🔑  PASSWORD RESET TOKEN  🔑🔑🔑                          ║\n" +
                "║                                                                  ║\n" +
                "║  📧 Email : {}  \n" +
                "║  🎫 Token : {}  \n" +
                "║                                                                  ║\n" +
                "║  ⏰ Expires in 1 hour                                            ║\n" +
                "║  📋 Copy this token and use it on the Reset Password page        ║\n" +
                "╚══════════════════════════════════════════════════════════════════╝",
                user.getEmail(), token);

        // In production, you would send this token via email
        // For now, we return it directly (FOR TESTING ONLY)
        return token;
    }

    // ─── Reset Password ────────────────────────────────────────────────────────

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {

        log.info("Password reset attempt with token: {}", req.getToken());

        PasswordResetToken resetToken = resetTokenRepository.findByToken(req.getToken())
                .orElseThrow(() -> {
                    log.warn("Invalid reset token: {}", req.getToken());
                    return new RuntimeException("Invalid or expired reset token");
                });

        if (resetToken.getUsed()) {
            log.warn("Reset token already used: {}", req.getToken());
            throw new RuntimeException("This reset token has already been used");
        }

        if (resetToken.isExpired()) {
            log.warn("Reset token expired: {}", req.getToken());
            throw new RuntimeException("Reset token has expired");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);

        log.info("Password reset successful for user: {}", user.getEmail());
    }

    // ─── Change Password (Authenticated User) ─────────────────────────────────

    @Transactional
    public void changePassword(User user, String currentPassword, String newPassword) {

        log.info("Change password request for user: {}", user.getEmail());

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            log.warn("Change password failed: incorrect current password for {}", user.getEmail());
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Encode and save new password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("✅ Password changed successfully for user: {}", user.getEmail());
    }
}
