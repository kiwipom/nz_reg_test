-- Add unique constraint to company_name
-- Author: Claude
-- Date: 2025-07-17
-- Reason: Company names should be unique in the companies register per business requirements

-- SAFE: Add unique constraint to company_name column
-- This enforces business rule that company names must be unique
-- Note: PostgreSQL automatically creates an index for unique constraints
ALTER TABLE companies ADD CONSTRAINT uk_companies_company_name UNIQUE (company_name);

-- Update comment to reflect the constraint
COMMENT ON COLUMN companies.company_name IS 'Company name - must be unique across all companies';