// ── DuplicateUserException.java ──────────────────────────────────────────────
package com.utkarsh.paytm_wallet_clone.exception;

public class DuplicateUserException extends RuntimeException {
    public DuplicateUserException(String message) {
        super(message);
    }
}