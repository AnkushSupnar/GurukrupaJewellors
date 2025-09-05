-- Quick fix for bill_transactions table
-- Run this SQL script to fix the immediate error

-- Option 1: Drop the old columns that are no longer in the entity
ALTER TABLE bill_transactions 
    DROP COLUMN IF EXISTS gold_value,
    DROP COLUMN IF EXISTS transaction_type,
    DROP COLUMN IF EXISTS jewelry_item_id,
    DROP COLUMN IF EXISTS quantity,
    DROP COLUMN IF EXISTS weight_per_unit,
    DROP COLUMN IF EXISTS total_weight,
    DROP COLUMN IF EXISTS stone_charges,
    DROP COLUMN IF EXISTS other_charges,
    DROP COLUMN IF EXISTS deduction_weight,
    DROP COLUMN IF EXISTS net_weight;

-- Option 2: If you can't drop columns yet, add default values
-- ALTER TABLE bill_transactions 
--     MODIFY COLUMN gold_value DECIMAL(12,2) DEFAULT 0.00;

-- Verify the table structure
DESCRIBE bill_transactions;