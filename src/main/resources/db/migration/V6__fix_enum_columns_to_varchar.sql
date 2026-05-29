-- Step 1: Remove defaults that depend on enum types
ALTER TABLE transactions ALTER COLUMN status DROP DEFAULT;
ALTER TABLE transactions ALTER COLUMN type   DROP DEFAULT;
ALTER TABLE ledger_entries ALTER COLUMN entry_type DROP DEFAULT;

-- Step 2: Convert columns to VARCHAR using CAST
ALTER TABLE transactions
    ALTER COLUMN type   TYPE VARCHAR(20) USING type::TEXT,
    ALTER COLUMN status TYPE VARCHAR(20) USING status::TEXT;

ALTER TABLE ledger_entries
    ALTER COLUMN entry_type TYPE VARCHAR(10) USING entry_type::TEXT;

-- Step 3: Now safe to drop the enum types
DROP TYPE IF EXISTS transaction_type;
DROP TYPE IF EXISTS transaction_status;
DROP TYPE IF EXISTS entry_type;