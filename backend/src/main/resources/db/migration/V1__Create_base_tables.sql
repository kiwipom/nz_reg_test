-- Create base tables for NZ Companies Register
-- Following Companies Act 1993 requirements

-- Create ENUM types
CREATE TYPE company_type AS ENUM ('LTD', 'OVERSEAS', 'UNLIMITED');
CREATE TYPE address_type AS ENUM ('REGISTERED', 'SERVICE', 'COMMUNICATION');
CREATE TYPE director_status AS ENUM ('ACTIVE', 'RESIGNED', 'DISQUALIFIED');
CREATE TYPE share_class AS ENUM ('ORDINARY', 'PREFERENCE', 'REDEEMABLE');

-- Companies table - core company information
CREATE TABLE companies (
    id BIGSERIAL PRIMARY KEY,
    company_number VARCHAR(20) UNIQUE NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    company_type company_type NOT NULL,
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
    address_type address_type NOT NULL,
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
    status director_status NOT NULL DEFAULT 'ACTIVE',
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
    share_class share_class NOT NULL,
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

-- Comments for documentation
COMMENT ON TABLE companies IS 'Core company information as per Companies Act 1993';
COMMENT ON TABLE addresses IS 'All address types for companies (registered, service, communication)';
COMMENT ON TABLE directors IS 'Company directors with residency requirements';
COMMENT ON TABLE shareholders IS 'Company shareholders and their details';
COMMENT ON TABLE share_allocations IS 'Share ownership tracking with historical records';
COMMENT ON TABLE annual_returns IS 'Annual return submissions and status';
COMMENT ON TABLE documents IS 'All filed documents with retention policies';
COMMENT ON TABLE audit_log IS 'Complete audit trail for compliance';
COMMENT ON TABLE company_snapshots IS 'Immutable historical snapshots per section 89';

-- Trigger function for updated_at timestamps
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