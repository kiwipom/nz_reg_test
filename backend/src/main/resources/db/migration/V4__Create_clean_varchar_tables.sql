-- Create tables with VARCHAR columns instead of PostgreSQL enums
-- This migration starts fresh with VARCHAR columns for better Hibernate compatibility

-- Clean up existing tables
DROP TABLE IF EXISTS audit_log CASCADE;
DROP TABLE IF EXISTS company_snapshots CASCADE;
DROP TABLE IF EXISTS annual_returns CASCADE;
DROP TABLE IF EXISTS documents CASCADE;
DROP TABLE IF EXISTS share_allocations CASCADE;
DROP TABLE IF EXISTS shareholders CASCADE;
DROP TABLE IF EXISTS directors CASCADE;
DROP TABLE IF EXISTS addresses CASCADE;
DROP TABLE IF EXISTS companies CASCADE;

-- Drop existing views
DROP VIEW IF EXISTS current_company_state CASCADE;
DROP VIEW IF EXISTS overdue_annual_returns CASCADE;

-- Drop existing functions
DROP FUNCTION IF EXISTS check_director_residency_requirement() CASCADE;
DROP FUNCTION IF EXISTS check_company_name_uniqueness() CASCADE;
DROP FUNCTION IF EXISTS check_annual_return_due_date() CASCADE;
DROP FUNCTION IF EXISTS check_share_allocation_integrity() CASCADE;
DROP FUNCTION IF EXISTS check_address_effective_dates() CASCADE;
DROP FUNCTION IF EXISTS check_required_addresses() CASCADE;
DROP FUNCTION IF EXISTS update_updated_at_column() CASCADE;

-- Drop existing enum types
DROP TYPE IF EXISTS company_type CASCADE;
DROP TYPE IF EXISTS address_type CASCADE;
DROP TYPE IF EXISTS director_status CASCADE;
DROP TYPE IF EXISTS share_class CASCADE;

-- Companies table - core company information
CREATE TABLE companies (
    id BIGSERIAL PRIMARY KEY,
    company_number VARCHAR(20) UNIQUE NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    company_type VARCHAR(20) NOT NULL CHECK (company_type IN ('LTD', 'OVERSEAS', 'UNLIMITED')),
    incorporation_date DATE NOT NULL,
    nzbn VARCHAR(13) UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 1
);

-- Addresses table - all address types for companies
CREATE TABLE addresses (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    address_type VARCHAR(20) NOT NULL CHECK (address_type IN ('REGISTERED', 'SERVICE', 'COMMUNICATION')),
    address_line_1 VARCHAR(255) NOT NULL,
    address_line_2 VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    region VARCHAR(100),
    postcode VARCHAR(10),
    country VARCHAR(2) NOT NULL DEFAULT 'NZ',
    email VARCHAR(255),
    phone VARCHAR(20),
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Directors table - company directors
CREATE TABLE directors (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    full_name VARCHAR(255) NOT NULL,
    date_of_birth DATE,
    place_of_birth VARCHAR(255),
    residential_address_line_1 VARCHAR(255) NOT NULL,
    residential_address_line_2 VARCHAR(255),
    residential_city VARCHAR(100) NOT NULL,
    residential_region VARCHAR(100),
    residential_postcode VARCHAR(10),
    residential_country VARCHAR(2) NOT NULL DEFAULT 'NZ',
    is_nz_resident BOOLEAN NOT NULL,
    is_australian_resident BOOLEAN NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'RESIGNED', 'DISQUALIFIED')),
    consent_given BOOLEAN NOT NULL DEFAULT FALSE,
    consent_date DATE,
    appointed_date DATE NOT NULL,
    resigned_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Ensure at least one NZ or Australian resident per company
    CONSTRAINT check_residency CHECK (is_nz_resident OR is_australian_resident)
);

-- Shareholders table - company shareholders
CREATE TABLE shareholders (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    full_name VARCHAR(255) NOT NULL,
    address_line_1 VARCHAR(255) NOT NULL,
    address_line_2 VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    region VARCHAR(100),
    postcode VARCHAR(10),
    country VARCHAR(2) NOT NULL DEFAULT 'NZ',
    is_individual BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Share allocations table - tracks share ownership
CREATE TABLE share_allocations (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    shareholder_id BIGINT NOT NULL REFERENCES shareholders(id),
    share_class VARCHAR(20) NOT NULL CHECK (share_class IN ('ORDINARY', 'PREFERENCE', 'REDEEMABLE')),
    number_of_shares BIGINT NOT NULL,
    share_value DECIMAL(15,2),
    allocation_date DATE NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT check_positive_shares CHECK (number_of_shares > 0),
    CONSTRAINT check_positive_value CHECK (share_value IS NULL OR share_value >= 0)
);

-- Annual returns table - tracks annual return submissions
CREATE TABLE annual_returns (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    return_date DATE NOT NULL,
    due_date DATE NOT NULL,
    submitted_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    filing_fee DECIMAL(10,2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Documents table - tracks all filed documents
CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    document_type VARCHAR(50) NOT NULL,
    document_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    mime_type VARCHAR(100),
    uploaded_by VARCHAR(255),
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_public BOOLEAN NOT NULL DEFAULT TRUE,
    retention_until DATE
);

-- Audit log table - tracks all changes for compliance
CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    table_name VARCHAR(50) NOT NULL,
    record_id BIGINT NOT NULL,
    action VARCHAR(10) NOT NULL, -- INSERT, UPDATE, DELETE
    old_values JSONB,
    new_values JSONB,
    changed_by VARCHAR(255),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address INET,
    user_agent TEXT
);

-- Historical snapshots table - immutable snapshots per section 89
CREATE TABLE company_snapshots (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    snapshot_date DATE NOT NULL,
    snapshot_data JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(company_id, snapshot_date)
);

-- Indexes for performance
CREATE INDEX idx_companies_company_number ON companies(company_number);
CREATE INDEX idx_companies_company_name ON companies(company_name);
CREATE INDEX idx_companies_incorporation_date ON companies(incorporation_date);
CREATE INDEX idx_companies_status ON companies(status);

CREATE INDEX idx_addresses_company_id ON addresses(company_id);
CREATE INDEX idx_addresses_type ON addresses(address_type);
CREATE INDEX idx_addresses_effective_dates ON addresses(effective_from, effective_to);

CREATE INDEX idx_directors_company_id ON directors(company_id);
CREATE INDEX idx_directors_full_name ON directors(full_name);
CREATE INDEX idx_directors_status ON directors(status);
CREATE INDEX idx_directors_appointed_date ON directors(appointed_date);

CREATE INDEX idx_shareholders_company_id ON shareholders(company_id);
CREATE INDEX idx_shareholders_full_name ON shareholders(full_name);

CREATE INDEX idx_share_allocations_company_id ON share_allocations(company_id);
CREATE INDEX idx_share_allocations_shareholder_id ON share_allocations(shareholder_id);
CREATE INDEX idx_share_allocations_effective_dates ON share_allocations(effective_from, effective_to);

CREATE INDEX idx_annual_returns_company_id ON annual_returns(company_id);
CREATE INDEX idx_annual_returns_due_date ON annual_returns(due_date);
CREATE INDEX idx_annual_returns_status ON annual_returns(status);

CREATE INDEX idx_documents_company_id ON documents(company_id);
CREATE INDEX idx_documents_type ON documents(document_type);
CREATE INDEX idx_documents_uploaded_at ON documents(uploaded_at);

CREATE INDEX idx_audit_log_table_record ON audit_log(table_name, record_id);
CREATE INDEX idx_audit_log_changed_at ON audit_log(changed_at);
CREATE INDEX idx_audit_log_changed_by ON audit_log(changed_by);

CREATE INDEX idx_company_snapshots_company_id ON company_snapshots(company_id);
CREATE INDEX idx_company_snapshots_date ON company_snapshots(snapshot_date);

-- Function to update updated_at timestamps
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at
CREATE TRIGGER update_companies_updated_at BEFORE UPDATE ON companies
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_addresses_updated_at BEFORE UPDATE ON addresses
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_directors_updated_at BEFORE UPDATE ON directors
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_shareholders_updated_at BEFORE UPDATE ON shareholders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_share_allocations_updated_at BEFORE UPDATE ON share_allocations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_annual_returns_updated_at BEFORE UPDATE ON annual_returns
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create view for current company state
CREATE VIEW current_company_state AS
SELECT 
    c.id,
    c.company_number,
    c.company_name,
    c.company_type,
    c.incorporation_date,
    c.nzbn,
    c.status,
    -- Current registered address
    ra.address_line_1 as registered_address_line_1,
    ra.address_line_2 as registered_address_line_2,
    ra.city as registered_city,
    ra.region as registered_region,
    ra.postcode as registered_postcode,
    ra.country as registered_country,
    -- Current service address
    sa.address_line_1 as service_address_line_1,
    sa.address_line_2 as service_address_line_2,
    sa.city as service_city,
    sa.region as service_region,
    sa.postcode as service_postcode,
    sa.country as service_country,
    -- Active directors count
    (SELECT COUNT(*) FROM directors d WHERE d.company_id = c.id AND d.status = 'ACTIVE') as active_directors_count,
    -- NZ/AU resident directors count
    (SELECT COUNT(*) FROM directors d WHERE d.company_id = c.id AND d.status = 'ACTIVE' AND (d.is_nz_resident OR d.is_australian_resident)) as resident_directors_count,
    c.created_at,
    c.updated_at
FROM companies c
LEFT JOIN addresses ra ON c.id = ra.company_id 
    AND ra.address_type = 'REGISTERED' 
    AND CURRENT_DATE BETWEEN ra.effective_from AND COALESCE(ra.effective_to, '9999-12-31')
LEFT JOIN addresses sa ON c.id = sa.company_id 
    AND sa.address_type = 'SERVICE' 
    AND CURRENT_DATE BETWEEN sa.effective_from AND COALESCE(sa.effective_to, '9999-12-31')
WHERE c.status = 'ACTIVE';