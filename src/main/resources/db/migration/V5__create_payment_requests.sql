CREATE TYPE payment_status AS ENUM (
    'INITIATED', 'PROCESSING', 'SUCCESS', 'FAILED', 'REFUNDED'
);

CREATE TYPE payment_provider AS ENUM ('RAZORGATE', 'STRIPEGATE', 'MANUAL');

CREATE TABLE payment_requests (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id      UUID REFERENCES transactions(id),
    provider            payment_provider   NOT NULL,
    provider_ref_id     VARCHAR(100),
    status              payment_status     NOT NULL DEFAULT 'INITIATED',
    amount              NUMERIC(19, 4)     NOT NULL,
    currency            VARCHAR(3)         NOT NULL DEFAULT 'INR',
    failure_reason      TEXT,
    retry_count         INT                NOT NULL DEFAULT 0,
    idempotency_key     VARCHAR(100)       NOT NULL UNIQUE,
    wallet_id           UUID               NOT NULL REFERENCES wallets(id),
    initiated_by        UUID               NOT NULL REFERENCES users(id),
    created_at          TIMESTAMP          NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP          NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_wallet   ON payment_requests(wallet_id);
CREATE INDEX idx_payment_user     ON payment_requests(initiated_by);
CREATE INDEX idx_payment_provider ON payment_requests(provider, status);