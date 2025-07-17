-- Update share_allocations table to match ShareAllocation entity
-- Author: Claude
-- Date: 2025-07-17
-- Reason: Add new columns for enhanced share allocation tracking

-- Add new columns for share allocation tracking
ALTER TABLE share_allocations ADD COLUMN nominal_value DECIMAL(19,2) NOT NULL DEFAULT 0.00;
ALTER TABLE share_allocations ADD COLUMN amount_paid DECIMAL(19,2) NOT NULL DEFAULT 0.00;
ALTER TABLE share_allocations ADD COLUMN transfer_date DATE;
ALTER TABLE share_allocations ADD COLUMN transfer_to_shareholder_id BIGINT;
ALTER TABLE share_allocations ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE share_allocations ADD COLUMN certificate_number VARCHAR(50);
ALTER TABLE share_allocations ADD COLUMN is_fully_paid BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE share_allocations ADD COLUMN restrictions TEXT;

-- Update the share_value column to be optional (can be calculated from nominal_value * number_of_shares)
ALTER TABLE share_allocations ALTER COLUMN share_value DROP NOT NULL;

-- Add foreign key constraint for transfer_to_shareholder_id
ALTER TABLE share_allocations ADD CONSTRAINT fk_share_allocations_transfer_to_shareholder 
    FOREIGN KEY (transfer_to_shareholder_id) REFERENCES shareholders(id);

-- Add unique constraint for certificate_number when not null
ALTER TABLE share_allocations ADD CONSTRAINT uk_share_allocations_certificate_number 
    UNIQUE (certificate_number);

-- Add check constraint for status
ALTER TABLE share_allocations ADD CONSTRAINT check_share_allocation_status 
    CHECK (status IN ('ACTIVE', 'TRANSFERRED', 'CANCELLED'));

-- Add check constraint for nominal_value
ALTER TABLE share_allocations ADD CONSTRAINT check_positive_nominal_value 
    CHECK (nominal_value > 0);

-- Add check constraint for amount_paid
ALTER TABLE share_allocations ADD CONSTRAINT check_non_negative_amount_paid 
    CHECK (amount_paid >= 0);

-- Create index for performance
CREATE INDEX idx_share_allocations_status ON share_allocations(status);
CREATE INDEX idx_share_allocations_transfer_date ON share_allocations(transfer_date);
CREATE INDEX idx_share_allocations_certificate_number ON share_allocations(certificate_number);
CREATE INDEX idx_share_allocations_transfer_to_shareholder ON share_allocations(transfer_to_shareholder_id);

-- Update existing data to set nominal_value based on share_value
UPDATE share_allocations 
SET nominal_value = COALESCE(share_value / GREATEST(number_of_shares, 1), 1.00)
WHERE nominal_value = 0.00;

-- Update existing data to set amount_paid equal to share_value initially
UPDATE share_allocations 
SET amount_paid = COALESCE(share_value, 0.00),
    is_fully_paid = (share_value IS NOT NULL AND share_value > 0)
WHERE amount_paid = 0.00;

-- Update comments
COMMENT ON COLUMN share_allocations.nominal_value IS 'Nominal value per share';
COMMENT ON COLUMN share_allocations.amount_paid IS 'Amount paid for the shares';
COMMENT ON COLUMN share_allocations.transfer_date IS 'Date when shares were transferred (if applicable)';
COMMENT ON COLUMN share_allocations.transfer_to_shareholder_id IS 'ID of shareholder who received the transfer';
COMMENT ON COLUMN share_allocations.status IS 'Status of the share allocation (ACTIVE, TRANSFERRED, CANCELLED)';
COMMENT ON COLUMN share_allocations.certificate_number IS 'Share certificate number (if issued)';
COMMENT ON COLUMN share_allocations.is_fully_paid IS 'Whether the shares are fully paid';
COMMENT ON COLUMN share_allocations.restrictions IS 'Any restrictions on the shares';