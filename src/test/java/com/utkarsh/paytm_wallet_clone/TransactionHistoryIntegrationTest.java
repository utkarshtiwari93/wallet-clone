package com.utkarsh.paytm_wallet_clone;

import com.utkarsh.paytm_wallet_clone.dto.request.TransferRequest;
import com.utkarsh.paytm_wallet_clone.dto.response.TransactionDTO;
import com.utkarsh.paytm_wallet_clone.model.User;
import com.utkarsh.paytm_wallet_clone.model.Wallet;
import com.utkarsh.paytm_wallet_clone.repository.UserRepository;
import com.utkarsh.paytm_wallet_clone.repository.WalletRepository;
import com.utkarsh.paytm_wallet_clone.service.TransactionService;
import com.utkarsh.paytm_wallet_clone.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class TransactionHistoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransferService transferService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        // Create User A (Alice) with ₹1000
        alice = new User();
        alice.setName("Alice");
        alice.setEmail("alice.test@example.com");
        alice.setPhone("9876543210");
        alice.setPasswordHash(passwordEncoder.encode("Test@1234"));
        alice.setIsActive(true);
        userRepository.save(alice);

        Wallet aliceWallet = new Wallet();
        aliceWallet.setUser(alice);
        aliceWallet.setBalance(new BigDecimal("1000.00"));
        walletRepository.save(aliceWallet);

        // Create User B (Bob) with ₹0
        bob = new User();
        bob.setName("Bob");
        bob.setEmail("bob.test@example.com");
        bob.setPhone("9999999999");
        bob.setPasswordHash(passwordEncoder.encode("Test@1234"));
        bob.setIsActive(true);
        userRepository.save(bob);

        Wallet bobWallet = new Wallet();
        bobWallet.setUser(bob);
        bobWallet.setBalance(BigDecimal.ZERO);
        walletRepository.save(bobWallet);
    }

    @Test
    void transferMoneyAndVerifyBothSeeTransaction() {
        // ── Alice transfers ₹100 to Bob ──────────────────────────────────────

        TransferRequest request = new TransferRequest();
        request.setRecipientPhone("9999999999");
        request.setAmount(new BigDecimal("100.00"));
        request.setNote("Test transfer");

        transferService.transfer(alice, request);

        // ── Verify Alice sees SENT transaction ───────────────────────────────

        Page<TransactionDTO> aliceHistory = transactionService.getTransactionHistory(alice, 0, 10);

        assertThat(aliceHistory.getContent()).isNotEmpty();
        TransactionDTO aliceTxn = aliceHistory.getContent().get(0);

        assertThat(aliceTxn.getDirection()).isEqualTo(TransactionDTO.TransactionDirection.SENT);
        assertThat(aliceTxn.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(aliceTxn.getCounterpartyName()).isEqualTo("Bob");
        assertThat(aliceTxn.getCounterpartyPhone()).isEqualTo("9999999999");
        assertThat(aliceTxn.getDescription()).contains("Test transfer");

        System.out.println("✅ Alice sees SENT transaction: -₹" + aliceTxn.getAmount());

        // ── Verify Bob sees RECEIVED transaction ─────────────────────────────

        Page<TransactionDTO> bobHistory = transactionService.getTransactionHistory(bob, 0, 10);

        assertThat(bobHistory.getContent()).isNotEmpty();
        TransactionDTO bobTxn = bobHistory.getContent().get(0);

        assertThat(bobTxn.getDirection()).isEqualTo(TransactionDTO.TransactionDirection.RECEIVED);
        assertThat(bobTxn.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(bobTxn.getCounterpartyName()).isEqualTo("Alice");
        assertThat(bobTxn.getCounterpartyPhone()).isEqualTo("9876543210");

        System.out.println("✅ Bob sees RECEIVED transaction: +₹" + bobTxn.getAmount());

        // ── Verify both reference the same transaction ───────────────────────

        assertThat(aliceTxn.getTxnRef()).isEqualTo(bobTxn.getTxnRef());

        System.out.println("✅ Same transaction reference: " + aliceTxn.getTxnRef());
    }

    @Test
    void getTransactionByRef_shouldReturnCorrectDirection() {
        // Transfer ₹50 from Alice to Bob
        TransferRequest request = new TransferRequest();
        request.setRecipientPhone("9999999999");
        request.setAmount(new BigDecimal("50.00"));

        transferService.transfer(alice, request);

        // Get Alice's history and extract txnRef
        Page<TransactionDTO> aliceHistory = transactionService.getTransactionHistory(alice, 0, 10);
        String txnRef = aliceHistory.getContent().get(0).getTxnRef();

        // Fetch by txnRef for Alice
        TransactionDTO aliceTxn = transactionService.getTransactionByRef(alice, txnRef)
                .orElseThrow();

        assertThat(aliceTxn.getDirection()).isEqualTo(TransactionDTO.TransactionDirection.SENT);

        // Fetch same txnRef for Bob
        TransactionDTO bobTxn = transactionService.getTransactionByRef(bob, txnRef)
                .orElseThrow();

        assertThat(bobTxn.getDirection()).isEqualTo(TransactionDTO.TransactionDirection.RECEIVED);

        System.out.println("✅ Single transaction shows different direction for sender vs receiver");
    }

    @Test
    void paginationWorks_shouldReturnLimitedResults() {
        // Create multiple transfers
        for (int i = 1; i <= 5; i++) {
            TransferRequest request = new TransferRequest();
            request.setRecipientPhone("9999999999");
            request.setAmount(new BigDecimal("10.00"));
            request.setNote("Transfer " + i);
            transferService.transfer(alice, request);
        }

        // Page 0, size 3 — should return 3 results
        Page<TransactionDTO> page1 = transactionService.getTransactionHistory(alice, 0, 3);
        assertThat(page1.getContent()).hasSize(3);
        assertThat(page1.getTotalElements()).isEqualTo(5);
        assertThat(page1.getTotalPages()).isEqualTo(2);

        // Page 1, size 3 — should return 2 results
        Page<TransactionDTO> page2 = transactionService.getTransactionHistory(alice, 1, 3);
        assertThat(page2.getContent()).hasSize(2);

        System.out.println("✅ Pagination works correctly");
    }
}