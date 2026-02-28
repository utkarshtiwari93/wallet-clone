package com.utkarsh.paytm_wallet_clone.controller;

import com.utkarsh.paytm_wallet_clone.dto.request.TransferRequest;
import com.utkarsh.paytm_wallet_clone.dto.response.TransactionDTO;
import com.utkarsh.paytm_wallet_clone.dto.response.TransferResponse;
import com.utkarsh.paytm_wallet_clone.dto.response.UserLookupDTO;
import com.utkarsh.paytm_wallet_clone.dto.response.WalletBalanceDTO;
import com.utkarsh.paytm_wallet_clone.model.User;
import com.utkarsh.paytm_wallet_clone.service.PdfReceiptService;
import com.utkarsh.paytm_wallet_clone.service.TransactionService;
import com.utkarsh.paytm_wallet_clone.service.TransferService;
import com.utkarsh.paytm_wallet_clone.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;
    private final TransferService transferService;
    private final TransactionService transactionService;
    private final PdfReceiptService pdfReceiptService;

    public WalletController(WalletService walletService,
                            TransferService transferService,
                            TransactionService transactionService,
                            PdfReceiptService pdfReceiptService) {
        this.walletService = walletService;
        this.transferService = transferService;
        this.transactionService = transactionService;
        this.pdfReceiptService = pdfReceiptService;
    }

    @GetMapping("/balance")
    public ResponseEntity<WalletBalanceDTO> getBalance(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(walletService.getBalance(user));
    }

    @GetMapping("/user/{phone}")
    public ResponseEntity<UserLookupDTO> lookupUserByPhone(
            @PathVariable String phone,
            @AuthenticationPrincipal User currentUser) {

        UserLookupDTO lookup = walletService.lookupUserByPhone(phone);
        return ResponseEntity.ok(lookup);
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(transferService.transfer(user, request));
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<TransactionDTO>> getTransactionHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User user) {

        Page<TransactionDTO> history = transactionService.getTransactionHistory(user, page, size);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/transactions/{txnRef}")
    public ResponseEntity<TransactionDTO> getTransactionByRef(
            @PathVariable String txnRef,
            @AuthenticationPrincipal User user) {

        return transactionService.getTransactionByRef(user, txnRef)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── NEW: Download PDF Receipt ─────────────────────────────────────────────

    @GetMapping("/receipt/{txnRef}")
    public ResponseEntity<byte[]> downloadReceipt(
            @PathVariable String txnRef,
            @AuthenticationPrincipal User user) {

        byte[] pdfBytes = pdfReceiptService.generateReceipt(txnRef, user);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "receipt_" + txnRef + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}
