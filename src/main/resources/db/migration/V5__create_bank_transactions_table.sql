-- Create bank_transactions table to track all bank account movements
CREATE TABLE IF NOT EXISTS bank_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bank_account_id BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    balance_after_transaction DECIMAL(12,2) NOT NULL,
    source VARCHAR(50) NOT NULL,
    reference_type VARCHAR(50),
    reference_id BIGINT,
    reference_number VARCHAR(100),
    transaction_reference VARCHAR(100),
    description VARCHAR(500),
    party VARCHAR(255),
    transaction_date DATETIME NOT NULL,
    is_reconciled BOOLEAN DEFAULT FALSE,
    reconciled_date DATETIME,
    reconciled_by VARCHAR(100),
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date DATETIME,
    
    FOREIGN KEY (bank_account_id) REFERENCES bank_accounts(id) ON DELETE CASCADE,
    
    INDEX idx_bank_account (bank_account_id),
    INDEX idx_transaction_date (transaction_date),
    INDEX idx_transaction_type (transaction_type),
    INDEX idx_source (source),
    INDEX idx_reference (reference_type, reference_id),
    INDEX idx_reconciled (is_reconciled),
    INDEX idx_reference_number (reference_number)
);

-- Add comments for clarity
COMMENT ON TABLE bank_transactions IS 'Tracks all bank account transactions including debits and credits';
COMMENT ON COLUMN bank_transactions.transaction_type IS 'DEBIT or CREDIT';
COMMENT ON COLUMN bank_transactions.source IS 'BILL_PAYMENT, PURCHASE_PAYMENT, EXCHANGE_PAYMENT, MANUAL_ENTRY, etc.';
COMMENT ON COLUMN bank_transactions.reference_type IS 'Type of reference: BILL, PURCHASE, EXCHANGE, etc.';
COMMENT ON COLUMN bank_transactions.reference_id IS 'ID of the referenced entity';
COMMENT ON COLUMN bank_transactions.reference_number IS 'Bill number, Purchase invoice number, etc.';
COMMENT ON COLUMN bank_transactions.transaction_reference IS 'Bank transaction ID or reference number';
COMMENT ON COLUMN bank_transactions.party IS 'Customer or supplier name';

-- Create a trigger to update bank account balance (optional - handled in service layer)
-- This is just for reference, actual balance update is done in the service
DELIMITER $$
CREATE TRIGGER update_bank_balance_after_transaction
AFTER INSERT ON bank_transactions
FOR EACH ROW
BEGIN
    -- This trigger is for documentation purposes
    -- Actual balance update is handled in the service layer
    -- to maintain transactional consistency
END$$
DELIMITER ;