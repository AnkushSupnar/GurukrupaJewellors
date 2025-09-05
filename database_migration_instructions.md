# Database Migration Instructions for Separate Transaction Tables

## Overview
We've separated BillTransaction and ExchangeTransaction into two different entities/tables for better data organization.

## SQL Migration Script

Run these SQL commands in order:

```sql
-- 1. First, fix the bill_amount issue if not already done
ALTER TABLE bills DROP COLUMN IF EXISTS bill_amount;

-- 2. Create the exchange_transactions table
CREATE TABLE IF NOT EXISTS exchange_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bill_id BIGINT NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    metal_type VARCHAR(100) NOT NULL,
    gross_weight DECIMAL(10,3) NOT NULL DEFAULT 0.000,
    deduction DECIMAL(10,3) DEFAULT 0.000,
    net_weight DECIMAL(10,3) NOT NULL DEFAULT 0.000,
    rate_per_ten_grams DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    created_date DATETIME NOT NULL,
    updated_date DATETIME,
    FOREIGN KEY (bill_id) REFERENCES bills(id) ON DELETE CASCADE
);

-- 3. Migrate existing exchange data from bill_transactions to exchange_transactions
-- (Only if you have existing data with transaction_type = 'EXCHANGE')
INSERT INTO exchange_transactions (
    bill_id,
    item_name,
    metal_type,
    gross_weight,
    deduction,
    net_weight,
    rate_per_ten_grams,
    total_amount,
    created_date,
    updated_date
)
SELECT 
    bill_id,
    item_name,
    metal_type,
    total_weight as gross_weight,
    COALESCE(deduction_weight, 0) as deduction,
    COALESCE(net_weight, total_weight) as net_weight,
    rate_per_ten_grams,
    total_amount,
    created_date,
    updated_date
FROM bill_transactions
WHERE transaction_type = 'EXCHANGE';

-- 4. Remove exchange transactions from bill_transactions
DELETE FROM bill_transactions WHERE transaction_type = 'EXCHANGE';

-- 5. Update bill_transactions table - remove unnecessary columns
ALTER TABLE bill_transactions 
    DROP COLUMN IF EXISTS transaction_type,
    DROP COLUMN IF EXISTS jewelry_item_id,
    DROP COLUMN IF EXISTS quantity,
    DROP COLUMN IF EXISTS weight_per_unit,
    DROP COLUMN IF EXISTS total_weight,
    DROP COLUMN IF EXISTS stone_charges,
    DROP COLUMN IF EXISTS other_charges,
    DROP COLUMN IF EXISTS gold_value,
    DROP COLUMN IF EXISTS deduction_weight,
    DROP COLUMN IF EXISTS net_weight;

-- 6. Rename columns if needed
ALTER TABLE bill_transactions 
    CHANGE COLUMN rate_per_ten_grams rate_per_ten_grams DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS weight DECIMAL(10,3) NOT NULL DEFAULT 0.000 AFTER metal_type;

-- 7. If you have data in total_weight, migrate it to weight
UPDATE bill_transactions SET weight = total_weight WHERE weight = 0 AND total_weight IS NOT NULL;

-- 8. Ensure all required columns have proper defaults
ALTER TABLE bill_transactions 
    MODIFY COLUMN item_code VARCHAR(100) NOT NULL,
    MODIFY COLUMN item_name VARCHAR(255) NOT NULL,
    MODIFY COLUMN metal_type VARCHAR(100) NOT NULL,
    MODIFY COLUMN weight DECIMAL(10,3) NOT NULL DEFAULT 0.000,
    MODIFY COLUMN rate_per_ten_grams DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    MODIFY COLUMN labour_charges DECIMAL(12,2) DEFAULT 0.00,
    MODIFY COLUMN total_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00;
```

## Verification Queries

After running the migration, verify with:

```sql
-- Check bill_transactions structure
DESCRIBE bill_transactions;

-- Check exchange_transactions structure  
DESCRIBE exchange_transactions;

-- Count records in each table
SELECT COUNT(*) as bill_transaction_count FROM bill_transactions;
SELECT COUNT(*) as exchange_transaction_count FROM exchange_transactions;

-- Verify bills table doesn't have bill_amount
SHOW COLUMNS FROM bills LIKE 'bill_amount';
```

## Rollback Script (if needed)

```sql
-- Add transaction_type back to bill_transactions
ALTER TABLE bill_transactions ADD COLUMN transaction_type VARCHAR(20);
UPDATE bill_transactions SET transaction_type = 'SALE';

-- Move exchange transactions back to bill_transactions
INSERT INTO bill_transactions (
    bill_id, item_code, item_name, metal_type, 
    rate_per_ten_grams, weight, labour_charges, 
    total_amount, transaction_type, created_date, updated_date
)
SELECT 
    bill_id,
    CONCAT('EXCHANGE-', id) as item_code,
    item_name,
    metal_type,
    rate_per_ten_grams,
    net_weight as weight,
    0 as labour_charges,
    total_amount,
    'EXCHANGE' as transaction_type,
    created_date,
    updated_date
FROM exchange_transactions;

-- Drop exchange_transactions table
DROP TABLE IF EXISTS exchange_transactions;
```

## Important Notes
1. **Backup your database** before running any migration
2. Stop the application during migration
3. Run the migration in a transaction if possible
4. Test in development environment first
5. After successful migration, restart the application