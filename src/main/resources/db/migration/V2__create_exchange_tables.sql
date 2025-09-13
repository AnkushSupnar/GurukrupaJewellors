-- Create Exchange table
CREATE TABLE exchanges (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exchange_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    total_exchange_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(50) NOT NULL,
    exchange_date DATETIME NOT NULL,
    created_date DATETIME NOT NULL,
    updated_date DATETIME,
    notes VARCHAR(500),
    bill_id BIGINT,
    CONSTRAINT fk_exchange_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
);

-- Add index on exchange_number for faster lookups
CREATE INDEX idx_exchange_number ON exchanges(exchange_number);

-- Add index on customer_id for faster customer-based queries
CREATE INDEX idx_exchange_customer ON exchanges(customer_id);

-- Add index on exchange_date for date-based queries
CREATE INDEX idx_exchange_date ON exchanges(exchange_date);

-- Add index on status
CREATE INDEX idx_exchange_status ON exchanges(status);

-- Modify exchange_transactions table to reference exchanges instead of bills
ALTER TABLE exchange_transactions DROP FOREIGN KEY IF EXISTS exchange_transactions_ibfk_1;
ALTER TABLE exchange_transactions DROP COLUMN IF EXISTS bill_id;
ALTER TABLE exchange_transactions ADD COLUMN exchange_id BIGINT NOT NULL AFTER id;
ALTER TABLE exchange_transactions ADD CONSTRAINT fk_exchange_transaction_exchange FOREIGN KEY (exchange_id) REFERENCES exchanges(id);

-- Add exchange_id to bills table for optional exchange reference
ALTER TABLE bills ADD COLUMN exchange_id BIGINT AFTER pending_amount;
ALTER TABLE bills ADD CONSTRAINT fk_bill_exchange FOREIGN KEY (exchange_id) REFERENCES exchanges(id);

-- Create index on bills.exchange_id
CREATE INDEX idx_bill_exchange ON bills(exchange_id);

-- Add foreign key constraint for bill_id in exchanges table (circular reference, but valid for tracking)
-- Note: This is added after bills table is modified to avoid foreign key issues
ALTER TABLE exchanges ADD CONSTRAINT fk_exchange_bill FOREIGN KEY (bill_id) REFERENCES bills(id);

-- Create index on exchanges.bill_id
CREATE INDEX idx_exchange_bill ON exchanges(bill_id);