-- This script updates the bills table to match the current entity structure
-- Remove the old bill_amount column if it exists and is not being used

-- First, check if the column exists and drop it
ALTER TABLE bills DROP COLUMN IF EXISTS bill_amount;

-- Ensure all required columns exist with proper constraints
ALTER TABLE bills 
    MODIFY COLUMN subtotal DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    MODIFY COLUMN cgst_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    MODIFY COLUMN sgst_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    MODIFY COLUMN total_tax_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    MODIFY COLUMN net_total DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    MODIFY COLUMN grand_total DECIMAL(12,2) NOT NULL DEFAULT 0.00;

-- Add any missing columns with defaults
ALTER TABLE bills 
    ADD COLUMN IF NOT EXISTS exchange_amount DECIMAL(12,2) DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS gst_rate DECIMAL(5,2) NOT NULL DEFAULT 3.00;