package com.utkarsh.paytm_wallet_clone.model.enums;

public enum TransactionType {
    CREDIT,    // money added to wallet (e.g. Razorpay top-up)
    DEBIT,     // money withdrawn from wallet
    TRANSFER   // wallet-to-wallet transfer
}