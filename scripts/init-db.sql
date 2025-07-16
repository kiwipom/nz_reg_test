-- Initial database setup for NZ Companies Register
-- This script is run when the PostgreSQL container starts

-- Create database if it doesn't exist
CREATE DATABASE nz_companies_register;

-- Grant privileges to postgres user
GRANT ALL PRIVILEGES ON DATABASE nz_companies_register TO postgres;

-- Connect to the database
\c nz_companies_register;

-- Create schema
CREATE SCHEMA IF NOT EXISTS companies_register;

-- Set search path
SET search_path TO companies_register, public;

-- Grant schema privileges
GRANT ALL ON SCHEMA companies_register TO postgres;
GRANT ALL ON SCHEMA public TO postgres;

-- Enable extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Create basic roles (these will be overridden by Flyway migrations)
DO $$
BEGIN
    -- This is just a placeholder - actual tables will be created by Flyway
    SELECT 1;
END $$;