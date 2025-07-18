export interface Company {
  id?: number;
  companyNumber: string;
  companyName: string;
  companyType: 'LTD' | 'OVERSEAS' | 'UNLIMITED';
  incorporationDate: string;
  nzbn?: string;
  status?: string;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
}

export interface CompanySearchResult {
  id: number;
  companyNumber: string;
  companyName: string;
  companyType: 'LTD' | 'OVERSEAS' | 'UNLIMITED';
  incorporationDate: string;
  nzbn?: string;
  status: string;
}

export interface CompanyDetails extends Company {
  directors?: Director[];
  shareholders?: Shareholder[];
  addresses?: Address[];
}

export interface Director {
  id: number;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  isNzResident: boolean;
  isAustralianResident: boolean;
  appointmentDate: string;
  resignationDate?: string;
  status: 'ACTIVE' | 'RESIGNED' | 'REMOVED';
}

export interface Shareholder {
  id: number;
  fullName: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  region?: string;
  postcode?: string;
  country: string;
  isIndividual: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ShareAllocation {
  id: number;
  company: Company;
  shareholder: Shareholder;
  shareClass: string;
  numberOfShares: number;
  nominalValue: number;
  amountPaid: number;
  allocationDate: string;
  transferDate?: string;
  transferToShareholderId?: number;
  status: 'ACTIVE' | 'TRANSFERRED' | 'CANCELLED';
  certificateNumber?: string;
  isFullyPaid: boolean;
  restrictions?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ShareAllocationRequest {
  companyId: number;
  shareholderId: number;
  shareClass: string;
  numberOfShares: number;
  nominalValue: number;
  amountPaid?: number;
  allocationDate?: string;
  certificateNumber?: string;
  restrictions?: string;
}

export interface ShareTransferRequest {
  toShareholderId: number;
  transferDate?: string;
  certificateNumber?: string;
}

export interface ShareholderPortfolio {
  totalShares: number;
  totalValue: number;
  totalPaid: number;
  unpaidAmount: number;
  shareClassBreakdown: Record<string, {
    totalShares: number;
    totalValue: number;
    totalPaid: number;
  }>;
  activeAllocations: number;
}

export interface ShareClass {
  id: number;
  companyId: number;
  className: string;
  classCode: string;
  description?: string;
  isRedeemable: boolean;
  isConvertible: boolean;
  parValue?: number;
  isNoParValue: boolean;
  currency: string;
  
  // Voting Rights
  votingRights: 'NONE' | 'ORDINARY' | 'WEIGHTED' | 'RESTRICTED';
  votesPerShare: number;
  votingRestrictions?: string;
  
  // Dividend Rights
  dividendRights: 'NONE' | 'ORDINARY' | 'PREFERRED' | 'CUMULATIVE';
  dividendRate?: number;
  isCumulativeDividend: boolean;
  dividendPriority: number;
  
  // Capital Distribution Rights
  capitalDistributionRights: 'ORDINARY' | 'PREFERRED' | 'NONE';
  liquidationPreferenceMultiple?: number;
  liquidationPriority: number;
  
  // Transfer Restrictions
  isTransferable: boolean;
  transferRestrictions?: string;
  requiresBoardApproval: boolean;
  hasPreemptiveRights: boolean;
  hasTagAlongRights: boolean;
  hasDragAlongRights: boolean;
  
  // Administrative
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  
  // Computed fields
  hasVotingRights: boolean;
  hasDividendRights: boolean;
  hasLiquidationPreference: boolean;
  effectiveVotesPerShare: number;
  canTransferFreely: boolean;
  displayName: string;
  rightsSummary: string;
}

export interface CreateShareClassRequest {
  className: string;
  classCode: string;
  description?: string;
  isRedeemable?: boolean;
  isConvertible?: boolean;
  parValue?: number;
  isNoParValue?: boolean;
  currency?: string;
  
  // Voting Rights
  votingRights?: 'NONE' | 'ORDINARY' | 'WEIGHTED' | 'RESTRICTED';
  votesPerShare?: number;
  votingRestrictions?: string;
  
  // Dividend Rights
  dividendRights?: 'NONE' | 'ORDINARY' | 'PREFERRED' | 'CUMULATIVE';
  dividendRate?: number;
  isCumulativeDividend?: boolean;
  dividendPriority?: number;
  
  // Capital Distribution Rights
  capitalDistributionRights?: 'ORDINARY' | 'PREFERRED' | 'NONE';
  liquidationPreferenceMultiple?: number;
  liquidationPriority?: number;
  
  // Transfer Restrictions
  isTransferable?: boolean;
  transferRestrictions?: string;
  requiresBoardApproval?: boolean;
  hasPreemptiveRights?: boolean;
  hasTagAlongRights?: boolean;
  hasDragAlongRights?: boolean;
}

export interface UpdateShareClassRequest {
  className?: string;
  description?: string;
  votingRights?: 'NONE' | 'ORDINARY' | 'WEIGHTED' | 'RESTRICTED';
  votesPerShare?: number;
  dividendRights?: 'NONE' | 'ORDINARY' | 'PREFERRED' | 'CUMULATIVE';
  dividendRate?: number;
  isCumulativeDividend?: boolean;
  dividendPriority?: number;
  capitalDistributionRights?: 'ORDINARY' | 'PREFERRED' | 'NONE';
  liquidationPreferenceMultiple?: number;
  liquidationPriority?: number;
  transferRestrictions?: string;
  requiresBoardApproval?: boolean;
  hasPreemptiveRights?: boolean;
  hasTagAlongRights?: boolean;
  hasDragAlongRights?: boolean;
}

export interface ShareClassStatistics {
  shareClassId: number;
  className: string;
  classCode: string;
  allocationCount: number;
  totalShares: number;
  totalValue: number;
}

export interface Address {
  id: number;
  addressType: 'REGISTERED' | 'SERVICE' | 'COMMUNICATION';
  addressLine1: string;
  addressLine2?: string;
  city: string;
  region: string;
  postalCode: string;
  country: string;
  effectiveDate: string;
  expiryDate?: string;
}