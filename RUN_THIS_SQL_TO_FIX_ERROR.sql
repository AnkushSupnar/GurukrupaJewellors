-- URGENT: Run this SQL to fix the application startup error
-- This adds the missing payment fields to the bills table

USE gurukrupa;

-- Add payment tracking fields to bills table
ALTER TABLE bills 
    ADD COLUMN IF NOT EXISTS paid_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS pending_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00;

-- Update existing bills to set paid amount equal to grand total for PAID status
UPDATE bills 
SET paid_amount = grand_total, 
    pending_amount = 0.00 
WHERE status = 'PAID' AND paid_amount = 0;

-- Update pending amounts for other statuses
UPDATE bills 
SET pending_amount = grand_total - paid_amount 
WHERE status NOT IN ('PAID', 'CANCELLED');

-- Verify the changes
DESCRIBE bills;