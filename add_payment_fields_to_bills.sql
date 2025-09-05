-- Add payment tracking fields to bills table
ALTER TABLE bills 
    ADD COLUMN paid_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN pending_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00;

-- Update existing bills to set paid amount equal to grand total for PAID status
UPDATE bills 
SET paid_amount = grand_total, 
    pending_amount = 0.00 
WHERE status = 'PAID';

-- Update pending amounts for other statuses
UPDATE bills 
SET pending_amount = grand_total - paid_amount 
WHERE status != 'PAID' AND status != 'CANCELLED';