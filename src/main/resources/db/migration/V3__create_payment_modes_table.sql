-- Create payment_modes table to track all payment methods used in bills
CREATE TABLE IF NOT EXISTS payment_modes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bill_id BIGINT NOT NULL,
    payment_type VARCHAR(50) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    reference_number VARCHAR(100),
    bank_account_id BIGINT,
    upi_id VARCHAR(100),
    card_last_four VARCHAR(4),
    card_type VARCHAR(20),
    card_network VARCHAR(30),
    payment_date DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    notes TEXT,
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date DATETIME,
    
    FOREIGN KEY (bill_id) REFERENCES bills(id) ON DELETE CASCADE,
    FOREIGN KEY (bank_account_id) REFERENCES bank_accounts(id) ON DELETE SET NULL,
    
    INDEX idx_payment_type (payment_type),
    INDEX idx_bill_id (bill_id),
    INDEX idx_payment_date (payment_date),
    INDEX idx_reference_number (reference_number)
);

-- Add comments for clarity
COMMENT ON TABLE payment_modes IS 'Tracks all payment methods used for bills including cash, bank, UPI, card etc.';
COMMENT ON COLUMN payment_modes.payment_type IS 'Type of payment: CASH, BANK, UPI, CARD, etc.';
COMMENT ON COLUMN payment_modes.reference_number IS 'Transaction reference number for non-cash payments';
COMMENT ON COLUMN payment_modes.bank_account_id IS 'Reference to bank account used for bank transfers';
COMMENT ON COLUMN payment_modes.upi_id IS 'UPI ID used for UPI payments';
COMMENT ON COLUMN payment_modes.card_last_four IS 'Last 4 digits of card for card payments';
COMMENT ON COLUMN payment_modes.card_type IS 'Card type: DEBIT, CREDIT';
COMMENT ON COLUMN payment_modes.card_network IS 'Card network: VISA, MASTERCARD, RUPAY, etc.';
COMMENT ON COLUMN payment_modes.status IS 'Payment status: COMPLETED, PENDING, FAILED, REFUNDED';