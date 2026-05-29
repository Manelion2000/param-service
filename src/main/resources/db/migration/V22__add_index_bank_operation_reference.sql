CREATE INDEX IF NOT EXISTS idx_bank_transaction_operation_reference
    ON bank_transaction(operation_reference);
