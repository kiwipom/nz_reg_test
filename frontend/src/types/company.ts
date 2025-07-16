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
  name: string;
  shareholderType: 'INDIVIDUAL' | 'COMPANY';
  numberOfShares: number;
  shareClass: string;
  allocationDate: string;
  transferDate?: string;
  status: 'ACTIVE' | 'TRANSFERRED';
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