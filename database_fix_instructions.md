# Database Schema Fix Instructions

## Problem
The database table `bills` has an old column `bill_amount` that no longer exists in the entity but is marked as NOT NULL without a default value, causing insert errors.

## Solution Options

### Option 1: Quick Fix (Recommended)
Run this SQL command in your MySQL database:

```sql
ALTER TABLE bills DROP COLUMN bill_amount;
```

### Option 2: Add Default Value (If you need to keep the column temporarily)
```sql
ALTER TABLE bills MODIFY COLUMN bill_amount DECIMAL(12,2) DEFAULT 0.00;
```

### Option 3: Complete Schema Update
Run all these commands to ensure your schema matches the entity:

```sql
-- Drop the old column
ALTER TABLE bills DROP COLUMN IF EXISTS bill_amount;

-- Ensure all required columns have proper defaults
ALTER TABLE bills 
    MODIFY COLUMN subtotal DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    MODIFY COLUMN cgst_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    MODIFY COLUMN sgst_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    MODIFY COLUMN total_tax_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    MODIFY COLUMN net_total DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    MODIFY COLUMN grand_total DECIMAL(12,2) NOT NULL DEFAULT 0.00;
```

## How to Execute
1. Open MySQL command line or a MySQL client (like MySQL Workbench)
2. Connect to your `gurukrupa` database
3. Run the SQL command(s) above
4. Restart your application

## Prevention
To prevent this in the future, consider:
1. Using database migrations (Flyway or Liquibase)
2. Setting `spring.jpa.hibernate.ddl-auto=create-drop` in development (WARNING: This drops all data)
3. Always updating entities and database schema together