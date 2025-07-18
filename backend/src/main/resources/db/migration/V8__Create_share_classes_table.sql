-- Create share_classes table for share class and rights management
-- Author: Claude
-- Date: 2025-07-18
-- Reason: Add proper share class management with voting, dividend, and liquidation rights

-- Create share_classes table
CREATE TABLE share_classes (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL,
    class_name VARCHAR(100) NOT NULL,
    class_code VARCHAR(20) NOT NULL,
    description VARCHAR(500),
    
    -- Basic share class properties
    is_redeemable BOOLEAN NOT NULL DEFAULT false,
    is_convertible BOOLEAN NOT NULL DEFAULT false,
    par_value DECIMAL(19,2),
    is_no_par_value BOOLEAN NOT NULL DEFAULT false,
    currency VARCHAR(3) NOT NULL DEFAULT 'NZD',
    
    -- Voting rights
    voting_rights VARCHAR(20) NOT NULL DEFAULT 'NONE',
    votes_per_share INTEGER NOT NULL DEFAULT 0,
    voting_restrictions VARCHAR(1000),
    
    -- Dividend rights
    dividend_rights VARCHAR(20) NOT NULL DEFAULT 'NONE',
    dividend_rate DECIMAL(5,4), -- e.g., 0.05 = 5%
    is_cumulative_dividend BOOLEAN NOT NULL DEFAULT false,
    dividend_priority INTEGER NOT NULL DEFAULT 0,
    
    -- Capital distribution rights
    capital_distribution_rights VARCHAR(20) NOT NULL DEFAULT 'ORDINARY',
    liquidation_preference_multiple DECIMAL(5,2), -- e.g., 1.5x preference
    liquidation_priority INTEGER NOT NULL DEFAULT 0,
    
    -- Transfer restrictions
    is_transferable BOOLEAN NOT NULL DEFAULT true,
    transfer_restrictions VARCHAR(1000),
    requires_board_approval BOOLEAN NOT NULL DEFAULT false,
    has_preemptive_rights BOOLEAN NOT NULL DEFAULT false,
    has_tag_along_rights BOOLEAN NOT NULL DEFAULT false,
    has_drag_along_rights BOOLEAN NOT NULL DEFAULT false,
    
    -- Administrative
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add foreign key constraints
ALTER TABLE share_classes ADD CONSTRAINT fk_share_classes_company 
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE;

-- Add unique constraints
ALTER TABLE share_classes ADD CONSTRAINT uk_share_classes_company_code 
    UNIQUE (company_id, class_code);

ALTER TABLE share_classes ADD CONSTRAINT uk_share_classes_company_name 
    UNIQUE (company_id, class_name);

-- Add check constraints
ALTER TABLE share_classes ADD CONSTRAINT check_voting_rights 
    CHECK (voting_rights IN ('NONE', 'ORDINARY', 'WEIGHTED', 'RESTRICTED'));

ALTER TABLE share_classes ADD CONSTRAINT check_dividend_rights 
    CHECK (dividend_rights IN ('NONE', 'ORDINARY', 'PREFERRED', 'CUMULATIVE'));

ALTER TABLE share_classes ADD CONSTRAINT check_capital_distribution_rights 
    CHECK (capital_distribution_rights IN ('ORDINARY', 'PREFERRED', 'NONE'));

ALTER TABLE share_classes ADD CONSTRAINT check_currency_code 
    CHECK (length(currency) = 3);

ALTER TABLE share_classes ADD CONSTRAINT check_votes_per_share_non_negative 
    CHECK (votes_per_share >= 0);

ALTER TABLE share_classes ADD CONSTRAINT check_dividend_rate_range 
    CHECK (dividend_rate IS NULL OR (dividend_rate >= 0 AND dividend_rate <= 1));

ALTER TABLE share_classes ADD CONSTRAINT check_liquidation_preference_positive 
    CHECK (liquidation_preference_multiple IS NULL OR liquidation_preference_multiple > 0);

ALTER TABLE share_classes ADD CONSTRAINT check_par_value_positive 
    CHECK (par_value IS NULL OR par_value > 0);

-- Logical constraints
ALTER TABLE share_classes ADD CONSTRAINT check_par_value_consistency 
    CHECK (
        (is_no_par_value = true AND par_value IS NULL) OR 
        (is_no_par_value = false)
    );

ALTER TABLE share_classes ADD CONSTRAINT check_voting_consistency 
    CHECK (
        (voting_rights = 'NONE' AND votes_per_share = 0) OR 
        (voting_rights != 'NONE' AND votes_per_share > 0)
    );

ALTER TABLE share_classes ADD CONSTRAINT check_dividend_rate_consistency 
    CHECK (
        (dividend_rights IN ('PREFERRED', 'CUMULATIVE') AND dividend_rate IS NOT NULL) OR 
        (dividend_rights NOT IN ('PREFERRED', 'CUMULATIVE'))
    );

-- Create indexes for performance
CREATE INDEX idx_share_classes_company_id ON share_classes(company_id);
CREATE INDEX idx_share_classes_active ON share_classes(is_active);
CREATE INDEX idx_share_classes_class_code ON share_classes(class_code);
CREATE INDEX idx_share_classes_voting_rights ON share_classes(voting_rights);
CREATE INDEX idx_share_classes_dividend_rights ON share_classes(dividend_rights);

-- Add comments
COMMENT ON TABLE share_classes IS 'Share classes with detailed rights and restrictions';
COMMENT ON COLUMN share_classes.class_name IS 'Human-readable name of the share class';
COMMENT ON COLUMN share_classes.class_code IS 'Short code identifier for the share class';
COMMENT ON COLUMN share_classes.voting_rights IS 'Type of voting rights (NONE, ORDINARY, WEIGHTED, RESTRICTED)';
COMMENT ON COLUMN share_classes.votes_per_share IS 'Number of votes per share';
COMMENT ON COLUMN share_classes.dividend_rights IS 'Type of dividend rights (NONE, ORDINARY, PREFERRED, CUMULATIVE)';
COMMENT ON COLUMN share_classes.dividend_rate IS 'Fixed dividend rate for preferred shares (as decimal, e.g., 0.05 = 5%)';
COMMENT ON COLUMN share_classes.liquidation_preference_multiple IS 'Liquidation preference multiple (e.g., 1.5 = 1.5x preference)';
COMMENT ON COLUMN share_classes.requires_board_approval IS 'Whether share transfers require board approval';

-- Insert default share classes for existing companies
INSERT INTO share_classes (
    company_id, 
    class_name, 
    class_code, 
    description,
    voting_rights,
    votes_per_share,
    dividend_rights,
    capital_distribution_rights
)
SELECT 
    c.id,
    'Ordinary Shares',
    'ORDINARY',
    'Standard ordinary shares with full voting rights and participation in dividends',
    'ORDINARY',
    1,
    'ORDINARY',
    'ORDINARY'
FROM companies c
WHERE NOT EXISTS (
    SELECT 1 FROM share_classes sc WHERE sc.company_id = c.id
);

-- Update existing share allocations to use proper share class codes
-- First, let's see what share classes exist in allocations
-- This will be handled in the service layer to maintain referential integrity