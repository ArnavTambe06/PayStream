CREATE TABLE wallets (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID           NOT NULL REFERENCES users(id),
    account_type VARCHAR(20)    NOT NULL DEFAULT 'SAVINGS',
    balance      NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    currency     VARCHAR(3)     NOT NULL DEFAULT 'INR',
    is_active    BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_balance_non_negative CHECK (balance >= 0),
    CONSTRAINT uq_user_account_type UNIQUE (user_id, account_type)
);

CREATE INDEX idx_wallets_user_id ON wallets(user_id);