-- First, drop the foreign key constraint from bills table to exchanges table
ALTER TABLE bills DROP FOREIGN KEY IF EXISTS bills_ibfk_3;
ALTER TABLE bills DROP FOREIGN KEY IF EXISTS FKexchange_id;
ALTER TABLE bills DROP FOREIGN KEY IF EXISTS bills_exchange_id_fkey;

-- Drop the exchange_id column from bills table
ALTER TABLE bills DROP COLUMN IF EXISTS exchange_id;

-- Now the circular dependency is removed
-- bills table no longer references exchanges table
-- exchanges table still references bills table (bill_id) which is fine