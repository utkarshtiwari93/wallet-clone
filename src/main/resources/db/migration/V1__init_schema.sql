-- ================================================
-- PayFlow Wallet — Initial Schema
-- V1__init_schema.sql
-- Run by Flyway automatically on app startup
-- ================================================

-- ── 1. USERS TABLE ──────────────────────────────
-- Stores all registered users
CREATE TABLE users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100)        NOT NULL,
    email       VARCHAR(150)        NOT NULL UNIQUE,
    phone       VARCHAR(15)         NOT NULL UNIQUE,
    password_hash VARCHAR(255)      NOT NULL,   -- BCrypt hash, never plain text
    is_active   BOOLEAN             DEFAULT TRUE,
    created_at  TIMESTAMP           DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP           DEFAULT CURRENT_TIMESTAMP
                                    ON UPDATE CURRENT_TIMESTAMP
);

-- ── 2. WALLETS TABLE ────────────────────────────
-- One wallet per user (1:1 relationship)
CREATE TABLE wallets (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT              NOT NULL UNIQUE,  -- UNIQUE enforces 1:1
    balance     DECIMAL(12, 2)      NOT NULL DEFAULT 0.00,
    currency    VARCHAR(3)          NOT NULL DEFAULT 'INR',
    is_active   BOOLEAN             DEFAULT TRUE,
    version     BIGINT              DEFAULT 0,        -- for optimistic locking
    created_at  TIMESTAMP           DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP           DEFAULT CURRENT_TIMESTAMP
                                    ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallet_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_balance_positive
        CHECK (balance >= 0)        -- NEVER allow negative balance at DB level
);

-- ── 3. TRANSACTIONS TABLE (LEDGER) ───────────────
-- Every balance change is RECORDED here — never deleted
-- This is your audit trail
CREATE TABLE transactions (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    txn_ref             VARCHAR(36)     NOT NULL UNIQUE,  -- UUID like 550e8400-e29b
    sender_wallet_id    BIGINT,                           -- NULL for deposits (no sender)
    receiver_wallet_id  BIGINT,                           -- NULL for withdrawals
    amount              DECIMAL(12, 2)  NOT NULL,
    type                ENUM('CREDIT', 'DEBIT', 'TRANSFER') NOT NULL,
    status              ENUM('PENDING', 'SUCCESS', 'FAILED') NOT NULL DEFAULT 'PENDING',
    description         VARCHAR(255),                     -- human readable note
    metadata            JSON,                             -- extra info (razorpay IDs etc)
    created_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_txn_sender
        FOREIGN KEY (sender_wallet_id) REFERENCES wallets(id),
    CONSTRAINT fk_txn_receiver
        FOREIGN KEY (receiver_wallet_id) REFERENCES wallets(id),
    CONSTRAINT chk_txn_amount
        CHECK (amount > 0)          -- amount must always be positive
);

-- ── 4. RAZORPAY ORDERS TABLE ─────────────────────
-- Tracks every payment attempt via Razorpay
-- Links a Razorpay order to a user and amount
CREATE TABLE razorpay_orders (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id    VARCHAR(100)    NOT NULL UNIQUE,   -- Razorpay order ID e.g. order_xxx
    payment_id  VARCHAR(100)    UNIQUE,            -- Razorpay payment ID — set after payment
    user_id     BIGINT          NOT NULL,
    amount      DECIMAL(12, 2)  NOT NULL,
    currency    VARCHAR(3)      DEFAULT 'INR',
    status      ENUM('CREATED', 'PAID', 'FAILED', 'REFUNDED')
                                DEFAULT 'CREATED',
    receipt     VARCHAR(150),                      -- your internal reference
    created_at  TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    paid_at     TIMESTAMP       NULL,              -- timestamp when payment confirmed
    CONSTRAINT fk_rzp_order_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);




-- ── INDEXES (for fast queries) ────────────────────
-- These speed up the most common lookups

-- Find wallet by user_id quickly (used every API call)
CREATE INDEX idx_wallets_user_id ON wallets(user_id);

-- Find transactions for a user's wallet
CREATE INDEX idx_txn_sender ON transactions(sender_wallet_id);
CREATE INDEX idx_txn_receiver ON transactions(receiver_wallet_id);
CREATE INDEX idx_txn_created_at ON transactions(created_at DESC);

-- Find Razorpay orders by user
CREATE INDEX idx_rzp_user_id ON razorpay_orders(user_id);
CREATE INDEX idx_rzp_status ON razorpay_orders(status);