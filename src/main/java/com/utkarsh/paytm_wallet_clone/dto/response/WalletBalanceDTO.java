package com.utkarsh.paytm_wallet_clone.dto.response;

import java.math.BigDecimal;

public class WalletBalanceDTO {

    private Long walletId;
    private BigDecimal balance;
    private String currency;
    private Long userId;
    private String userName;

    public WalletBalanceDTO(Long walletId, BigDecimal balance, String currency,
                            Long userId, String userName) {
        this.walletId = walletId;
        this.balance = balance;
        this.currency = currency;
        this.userId = userId;
        this.userName = userName;
    }

    public Long getWalletId() { return walletId; }
    public BigDecimal getBalance() { return balance; }
    public String getCurrency() { return currency; }
    public Long getUserId() { return userId; }
    public String getUserName() { return userName; }
}