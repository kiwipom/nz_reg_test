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