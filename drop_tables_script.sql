-- Script to drop tables in correct order to avoid foreign key constraint issues
-- Run this script if you need to completely reset the database

-- First, drop tables that depend on others
DROP TABLE IF EXISTS payment_modes;
DROP TABLE IF EXISTS bill_transactions;
DROP TABLE IF EXISTS exchange_transactions;

-- Now drop exchanges table (depends on bills)
DROP TABLE IF EXISTS exchanges;

-- Now drop bills table (no more dependencies)
DROP TABLE IF EXISTS bills;

-- Drop other independent tables if needed
-- DROP TABLE IF EXISTS customers;
-- DROP TABLE IF EXISTS jewelry_items;
-- DROP TABLE IF EXISTS metals;
-- DROP TABLE IF EXISTS metal_rates;
-- DROP TABLE IF EXISTS bank_accounts;
-- DROP TABLE IF EXISTS app_settings;