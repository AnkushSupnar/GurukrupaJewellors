-- Fix payment_method column to accept new enum values
USE gurukrupa;

-- First, check the current column definition
SHOW COLUMNS FROM bills WHERE Field = 'payment_method';

-- Update the payment_method column to include new enum values
ALTER TABLE bills 
MODIFY COLUMN payment_method ENUM('CASH', 'UPI', 'CARD', 'CHEQUE', 'BANK_TRANSFER', 'PARTIAL', 'CREDIT') NOT NULL;

-- Verify the change
SHOW COLUMNS FROM bills WHERE Field = 'payment_method';

-- If you have existing data, you might want to check for any issues
SELECT DISTINCT payment_method FROM bills;