CREATE TYPE transaction_type AS ENUM ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER_IN', 'TRANSFER_OUT');
CREATE TYPE transaction_status AS ENUM ('PENDING', 'SUCCESS', 'FAILED', 'REVERSED');
CREATE TYPE entry_type AS ENUM ('DEBIT', 'CREDIT');

-- Master transaction record
CREATE TABLE transactions (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_id     VARCHAR(100) NOT NULL UNIQUE,
    type             transaction_type   NOT NULL,
    status           transaction_status NOT NULL DEFAULT 'PENDING',
    amount           NUMERIC(19, 4)     NOT NULL,
    currency         VARCHAR(3)         NOT NULL DEFAULT 'INR',
    description      VARCHAR(255),
    from_wallet_id   UUID REFERENCES wallets(id),
    to_wallet_id     UUID REFERENCES wallets(id),
    initiated_by     UUID NOT NULL REFERENCES users(id),
    idempotency_key  VARCHAR(100) UNIQUE,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Double-entry ledger entries (one transaction = two ledger entries)
CREATE TABLE ledger_entries (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL REFERENCES transactions(id),
    wallet_id      UUID NOT NULL REFERENCES wallets(id),
    entry_type     entry_type     NOT NULL,
    amount         NUMERIC(19, 4) NOT NULL,
    balance_before NUMERIC(19, 4) NOT NULL,
    balance_after  NUMERIC(19, 4) NOT NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_txn_from_wallet   ON transactions(from_wallet_id);
CREATE INDEX idx_txn_to_wallet     ON transactions(to_wallet_id);
CREATE INDEX idx_txn_initiated_by  ON transactions(initiated_by);
CREATE INDEX idx_ledger_wallet_id  ON ledger_entries(wallet_id);
CREATE INDEX idx_txn_idempotency   ON transactions(idempotency_key);