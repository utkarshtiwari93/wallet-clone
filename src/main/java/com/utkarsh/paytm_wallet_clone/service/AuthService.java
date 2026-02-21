package com.utkarsh.paytm_wallet_clone.service;

import com.utkarsh.paytm_wallet_clone.dto.request.LoginRequest;
import com.utkarsh.paytm_wallet_clone.dto.request.RegisterRequest;
import com.utkarsh.paytm_wallet_clone.dto.response.AuthResponse;
import com.utkarsh.paytm_wallet_clone.exception.DuplicateUserException;
import com.utkarsh.paytm_wallet_clone.exception.UserNotFoundException;
import com.utkarsh.paytm_wallet_clone.model.User;
import com.utkarsh.paytm_wallet_clone.model.Wallet;
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

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       WalletRepository walletRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    // ─── Register ────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse register(RegisterRequest req) {

        log.info("Registration attempt for email: {}", req.getEmail());

        // 1. Check duplicates
        if (userRepository.existsByEmail(req.getEmail())) {
            log.warn("Registration failed: Email already exists - {}", req.getEmail());
            throw new DuplicateUserException("Email already registered: " + req.getEmail());
        }
        if (userRepository.existsByPhone(req.getPhone())) {
            log.warn("Registration failed: Phone already exists - {}", req.getPhone());
            throw new DuplicateUserException("Phone already registered: " + req.getPhone());
        }

        // 2. Create user
        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setIsActive(true);
        userRepository.save(user);

        log.info("User registered successfully: {} (ID: {})", user.getEmail(), user.getId());

        // 3. Auto-create wallet
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        walletRepository.save(wallet);

        log.info("Wallet created for user: {} (Wallet ID: {})", user.getEmail(), wallet.getId());

        // 4. Generate JWT
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        return new AuthResponse(token, user.getEmail(), user.getId(), user.getName());
    }

    // ─── Login ───────────────────────────────────────────────────────────────

    public AuthResponse login(LoginRequest req) {

        log.info("Login attempt for email: {}", req.getEmail());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
            );
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
}