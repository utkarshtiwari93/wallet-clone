package com.utkarsh.paytm_wallet_clone.service;

import com.utkarsh.paytm_wallet_clone.dto.request.TransferRequest;
import com.utkarsh.paytm_wallet_clone.dto.response.TransferResponse;
import com.utkarsh.paytm_wallet_clone.exception.InsufficientFundsException;
import com.utkarsh.paytm_wallet_clone.exception.UserNotFoundException;
import com.utkarsh.paytm_wallet_clone.model.User;
import com.utkarsh.paytm_wallet_clone.model.Wallet;
import com.utkarsh.paytm_wallet_clone.repository.UserRepository;
import com.utkarsh.paytm_wallet_clone.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransferService transferService;

    private User sender;
    private User recipient;
    private Wallet senderWallet;
    private Wallet recipientWallet;

    @BeforeEach
    void setUp() {
        // Sender: User A with ₹1000
        sender = new User();
        sender.setId(1L);
        sender.setName("Alice");
        sender.setPhone("9876543210");
        sender.setEmail("alice@test.com");

        senderWallet = new Wallet();
        senderWallet.setId(1L);
        senderWallet.setUser(sender);
        senderWallet.setBalance(new BigDecimal("1000.00"));

        // Recipient: User B with ₹500
        recipient = new User();
        recipient.setId(2L);
        recipient.setName("Bob");
        recipient.setPhone("9999999999");
        recipient.setEmail("bob@test.com");

        recipientWallet = new Wallet();
        recipientWallet.setId(2L);
        recipientWallet.setUser(recipient);
        recipientWallet.setBalance(new BigDecimal("500.00"));
    }

    // ─── Test 1: Successful Transfer ──────────────────────────────────────────

    @Test
    void transfer_success_shouldUpdateBothWallets() {
        TransferRequest request = new TransferRequest();
        request.setRecipientPhone("9999999999");
        request.setAmount(new BigDecimal("500.00"));
        request.setNote("Test transfer");

        when(userRepository.findByPhone("9999999999")).thenReturn(Optional.of(recipient));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUserId(2L)).thenReturn(Optional.of(recipientWallet));
        when(walletRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(recipientWallet));

        TransferResponse response = transferService.transfer(sender, request);

        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(response.getNewBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(senderWallet.getBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(recipientWallet.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));

        System.out.println("✅ Test passed: Successful transfer");
    }

    // ─── Test 2: Insufficient Funds ───────────────────────────────────────────

    @Test
    void transfer_insufficientFunds_shouldThrowException() {
        TransferRequest request = new TransferRequest();
        request.setRecipientPhone("9999999999");
        request.setAmount(new BigDecimal("1500.00")); // More than sender has

        when(userRepository.findByPhone("9999999999")).thenReturn(Optional.of(recipient));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUserId(2L)).thenReturn(Optional.of(recipientWallet));
        when(walletRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(recipientWallet));

        assertThatThrownBy(() -> transferService.transfer(sender, request))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds");

        System.out.println("✅ Test passed: Insufficient funds blocked");
    }

    // ─── Test 3: Self-Transfer Blocked ────────────────────────────────────────

    @Test
    void transfer_toSelf_shouldThrowException() {
        TransferRequest request = new TransferRequest();
        request.setRecipientPhone("9876543210"); // Sender's own phone
        request.setAmount(new BigDecimal("100.00"));

        assertThatThrownBy(() -> transferService.transfer(sender, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot transfer to yourself");

        System.out.println("✅ Test passed: Self-transfer blocked");
    }

    // ─── Test 4: Invalid Recipient Phone ──────────────────────────────────────

    @Test
    void transfer_invalidPhone_shouldThrowException() {
        TransferRequest request = new TransferRequest();
        request.setRecipientPhone("8888888888"); // Non-existent phone
        request.setAmount(new BigDecimal("100.00"));

        when(userRepository.findByPhone("8888888888")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.transfer(sender, request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("Recipient not found");

        System.out.println("✅ Test passed: Invalid recipient blocked");
    }

    // ─── Test 5: Exactly ₹0.01 Transfer ───────────────────────────────────────

    @Test
    void transfer_oneRupee_shouldWork() {
        TransferRequest request = new TransferRequest();
        request.setRecipientPhone("9999999999");
        request.setAmount(new BigDecimal("0.01"));

        when(userRepository.findByPhone("9999999999")).thenReturn(Optional.of(recipient));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUserId(2L)).thenReturn(Optional.of(recipientWallet));
        when(walletRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(recipientWallet));

        TransferResponse response = transferService.transfer(sender, request);

        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("0.01"));
        assertThat(senderWallet.getBalance()).isEqualByComparingTo(new BigDecimal("999.99"));
        assertThat(recipientWallet.getBalance()).isEqualByComparingTo(new BigDecimal("500.01"));

        System.out.println("✅ Test passed: Minimum amount transfer works");
    }
}