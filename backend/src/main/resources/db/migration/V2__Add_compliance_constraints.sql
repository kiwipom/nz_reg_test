-- Add compliance constraints and validation rules per Companies Act 1993

-- Function to ensure at least one NZ/Australian resident director per company
CREATE OR REPLACE FUNCTION check_director_residency_requirement()
RETURNS TRIGGER AS $$
BEGIN
    -- Check if there's at least one NZ or Australian resident director
    IF NOT EXISTS (
        SELECT 1 FROM directors 
        WHERE company_id = COALESCE(NEW.company_id, OLD.company_id)
        AND status = 'ACTIVE'
        AND (is_nz_resident = TRUE OR is_australian_resident = TRUE)
        AND id != COALESCE(OLD.id, 0)
    ) THEN
        RAISE EXCEPTION 'Company must have at least one NZ or Australian resident director';
    END IF;
    
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- Create trigger for director residency requirement
CREATE TRIGGER enforce_director_residency_requirement
    AFTER INSERT OR UPDATE OR DELETE ON directors
    FOR EACH ROW
    EXECUTE FUNCTION check_director_residency_requirement();

-- Function to validate company name uniqueness
CREATE OR REPLACE FUNCTION check_company_name_uniqueness()
RETURNS TRIGGER AS $$
BEGIN
    -- Check if company name already exists (case-insensitive)
    IF EXISTS (
        SELECT 1 FROM companies 
        WHERE LOWER(company_name) = LOWER(NEW.company_name)
        AND status = 'ACTIVE'
        AND id != NEW.id
    ) THEN
        RAISE EXCEPTION 'Company name already exists: %', NEW.company_name;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for company name uniqueness
CREATE TRIGGER enforce_company_name_uniqueness
    BEFORE INSERT OR UPDATE ON companies
    FOR EACH ROW
    EXECUTE FUNCTION check_company_name_uniqueness();

-- Function to validate annual return due dates
CREATE OR REPLACE FUNCTION check_annual_return_due_date()
RETURNS TRIGGER AS $$
DECLARE
    incorporation_date DATE;
BEGIN
    -- Get company incorporation date
    SELECT c.incorporation_date INTO incorporation_date
    FROM companies c
    WHERE c.id = NEW.company_id;
    
    -- Due date should be anniversary of incorporation
    IF NEW.due_date != (incorporation_date + INTERVAL '1 year' * 
        EXTRACT(YEAR FROM AGE(NEW.return_date, incorporation_date))) THEN
        RAISE EXCEPTION 'Annual return due date must be anniversary of incorporation';
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for annual return due date validation
CREATE TRIGGER validate_annual_return_due_date
    BEFORE INSERT OR UPDATE ON annual_returns
    FOR EACH ROW
    EXECUTE FUNCTION check_annual_return_due_date();

-- Function to ensure share allocation integrity
CREATE OR REPLACE FUNCTION check_share_allocation_integrity()
RETURNS TRIGGER AS $$
BEGIN
    -- Ensure effective_from <= effective_to
    IF NEW.effective_to IS NOT NULL AND NEW.effective_from > NEW.effective_to THEN
        RAISE EXCEPTION 'Share allocation effective_from must be before effective_to';
    END IF;
    
    -- Ensure allocation_date <= effective_from
    IF NEW.allocation_date > NEW.effective_from THEN
        RAISE EXCEPTION 'Share allocation date must be before or equal to effective_from';
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for share allocation integrity
CREATE TRIGGER validate_share_allocation_integrity
    BEFORE INSERT OR UPDATE ON share_allocations
    FOR EACH ROW
    EXECUTE FUNCTION check_share_allocation_integrity();

-- Function to ensure address effective date integrity
CREATE OR REPLACE FUNCTION check_address_effective_dates()
RETURNS TRIGGER AS $$
BEGIN
    -- Ensure effective_from <= effective_to
    IF NEW.effective_to IS NOT NULL AND NEW.effective_from > NEW.effective_to THEN
        RAISE EXCEPTION 'Address effective_from must be before effective_to';
    END IF;
    
    -- Ensure no overlapping effective dates for same address type
    IF EXISTS (
        SELECT 1 FROM addresses a
        WHERE a.company_id = NEW.company_id
        AND a.address_type = NEW.address_type
        AND a.id != NEW.id
        AND (
            (NEW.effective_from BETWEEN a.effective_from AND COALESCE(a.effective_to, '9999-12-31'))
            OR (COALESCE(NEW.effective_to, '9999-12-31') BETWEEN a.effective_from AND COALESCE(a.effective_to, '9999-12-31'))
            OR (NEW.effective_from <= a.effective_from AND COALESCE(NEW.effective_to, '9999-12-31') >= COALESCE(a.effective_to, '9999-12-31'))
        )
    ) THEN
        RAISE EXCEPTION 'Address effective dates cannot overlap for same address type';
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for address effective date integrity
CREATE TRIGGER validate_address_effective_dates
    BEFORE INSERT OR UPDATE ON addresses
    FOR EACH ROW
    EXECUTE FUNCTION check_address_effective_dates();

-- Function to ensure company has required address types
CREATE OR REPLACE FUNCTION check_required_addresses()
RETURNS TRIGGER AS $$
BEGIN
    -- Check if company has registered address
    IF NOT EXISTS (
        SELECT 1 FROM addresses a
        WHERE a.company_id = NEW.company_id
        AND a.address_type = 'REGISTERED'
        AND NEW.effective_from BETWEEN a.effective_from AND COALESCE(a.effective_to, '9999-12-31')
    ) THEN
        RAISE EXCEPTION 'Company must have a registered address';
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Add constraints for data integrity
ALTER TABLE companies
ADD CONSTRAINT check_company_number_format 
CHECK (company_number ~ '^[0-9]{1,8}$'),
ADD CONSTRAINT check_nzbn_format 
CHECK (nzbn IS NULL OR nzbn ~ '^[0-9]{13}$'),
ADD CONSTRAINT check_status_values 
CHECK (status IN ('ACTIVE', 'REMOVED', 'LIQUIDATION', 'RECEIVERSHIP'));

ALTER TABLE directors
ADD CONSTRAINT check_director_names 
CHECK (LENGTH(full_name) >= 2),
ADD CONSTRAINT check_appointment_resignation_dates 
CHECK (resigned_date IS NULL OR resigned_date >= appointed_date),
ADD CONSTRAINT check_consent_date 
CHECK (consent_date IS NULL OR consent_date >= appointed_date);

ALTER TABLE shareholders
ADD CONSTRAINT check_shareholder_names 
CHECK (LENGTH(full_name) >= 2);

ALTER TABLE annual_returns
ADD CONSTRAINT check_return_dates 
CHECK (submitted_date IS NULL OR submitted_date >= return_date),
ADD CONSTRAINT check_due_date 
CHECK (due_date >= return_date),
ADD CONSTRAINT check_filing_fee 
CHECK (filing_fee IS NULL OR filing_fee >= 0),
ADD CONSTRAINT check_status_values 
CHECK (status IN ('PENDING', 'SUBMITTED', 'OVERDUE', 'STRIKE_OFF'));

ALTER TABLE documents
ADD CONSTRAINT check_file_size 
CHECK (file_size IS NULL OR file_size > 0),
ADD CONSTRAINT check_document_name 
CHECK (LENGTH(document_name) >= 1);

-- Add additional indexes for compliance queries
CREATE INDEX idx_companies_status_incorporation ON companies(status, incorporation_date);
CREATE INDEX idx_directors_company_residency ON directors(company_id, is_nz_resident, is_australian_resident) WHERE status = 'ACTIVE';
CREATE INDEX idx_annual_returns_overdue ON annual_returns(due_date, status) WHERE status IN ('PENDING', 'OVERDUE');
CREATE INDEX idx_addresses_company_type_effective ON addresses(company_id, address_type, effective_from, effective_to);

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

-- Create view for overdue annual returns
CREATE VIEW overdue_annual_returns AS
SELECT 
    ar.id,
    ar.company_id,
    c.company_number,
    c.company_name,
    ar.return_date,
    ar.due_date,
    ar.status,
    CURRENT_DATE - ar.due_date as days_overdue,
    CASE 
        WHEN CURRENT_DATE - ar.due_date > 180 THEN 'STRIKE_OFF_ELIGIBLE'
        WHEN CURRENT_DATE - ar.due_date > 30 THEN 'OVERDUE'
        WHEN CURRENT_DATE - ar.due_date > 0 THEN 'RECENTLY_OVERDUE'
        ELSE 'CURRENT'
    END as overdue_status
FROM annual_returns ar
JOIN companies c ON ar.company_id = c.id
WHERE ar.status IN ('PENDING', 'OVERDUE')
AND ar.due_date < CURRENT_DATE;

-- Grant permissions for application user
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO app_user;
-- GRANT SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public TO app_user;
-- GRANT SELECT ON current_company_state TO app_user;
-- GRANT SELECT ON overdue_annual_returns TO app_user;