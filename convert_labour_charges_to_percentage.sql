-- Migration script to convert existing labour charges from fixed amount to percentage
-- This script calculates the percentage based on the gold value for each item

-- First, let's create a backup of the current labour charges
ALTER TABLE jewelry_items ADD COLUMN labour_charges_backup DECIMAL(12,2);
UPDATE jewelry_items SET labour_charges_backup = labour_charges;

-- Now convert labour charges to percentage
-- Formula: percentage = (labour_charges / gold_value) * 100
-- where gold_value = (net_weight * gold_rate) / 10
UPDATE jewelry_items 
SET labour_charges = ROUND(
    (labour_charges / ((net_weight * gold_rate) / 10)) * 100, 
    2
)
WHERE net_weight > 0 
  AND gold_rate > 0 
  AND labour_charges > 0;

-- For items where calculation isn't possible, set a default percentage
-- You can adjust this default value based on your business needs
UPDATE jewelry_items 
SET labour_charges = 10.00  -- Default 10% labour charges
WHERE (net_weight IS NULL OR net_weight = 0 
   OR gold_rate IS NULL OR gold_rate = 0 
   OR labour_charges IS NULL OR labour_charges = 0);

-- Verify the conversion
SELECT 
    item_code,
    item_name,
    net_weight,
    gold_rate,
    labour_charges_backup as old_labour_charges,
    labour_charges as new_labour_percentage,
    ROUND(((net_weight * gold_rate) / 10) * labour_charges / 100, 2) as calculated_labour_amount
FROM jewelry_items
LIMIT 10;

-- If everything looks good, you can drop the backup column
-- ALTER TABLE jewelry_items DROP COLUMN labour_charges_backup;

-- Note: After running this migration, all new entries will store labour charges as percentage
-- The application code has been updated to handle this change