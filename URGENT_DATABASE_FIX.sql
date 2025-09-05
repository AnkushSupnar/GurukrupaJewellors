-- URGENT: Fix for bill generation error
-- Run these commands in MySQL to fix the immediate issue

USE gurukrupa;

-- 1. First, add default values to prevent errors
ALTER TABLE bill_transactions 
    MODIFY COLUMN gold_value DECIMAL(12,2) DEFAULT 0.00,
    MODIFY COLUMN quantity INT DEFAULT 1,
    MODIFY COLUMN weight_per_unit DECIMAL(10,3) DEFAULT 0.000,
    MODIFY COLUMN total_weight DECIMAL(10,3) DEFAULT 0.000,
    MODIFY COLUMN stone_charges DECIMAL(12,2) DEFAULT 0.00,
    MODIFY COLUMN other_charges DECIMAL(12,2) DEFAULT 0.00,
    MODIFY COLUMN deduction_weight DECIMAL(10,3) DEFAULT 0.000,
    MODIFY COLUMN net_weight DECIMAL(10,3) DEFAULT 0.000;

-- 2. After verifying the application works, you can drop these columns
-- ALTER TABLE bill_transactions 
--     DROP COLUMN gold_value,
--     DROP COLUMN transaction_type,
--     DROP COLUMN jewelry_item_id,
--     DROP COLUMN quantity,
--     DROP COLUMN weight_per_unit,
--     DROP COLUMN total_weight,
--     DROP COLUMN stone_charges,
--     DROP COLUMN other_charges,
--     DROP COLUMN deduction_weight,
--     DROP COLUMN net_weight;

-- 3. Also fix the bills table issue if not done
ALTER TABLE bills DROP COLUMN IF EXISTS bill_amount;

-- 4. Check the current structure
DESCRIBE bill_transactions;