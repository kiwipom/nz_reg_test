import { Company } from '../types/company';

export interface CompanyRegistrationRequest {
  companyNumber: string;
  companyName: string;
  companyType: 'LTD' | 'OVERSEAS' | 'UNLIMITED';
  incorporationDate: string;
  nzbn?: string;
}

export interface CompanyRegistrationResponse extends Company {
  id: number;
  status: string;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export class RegistrationService {
  private readonly baseUrl = 'http://localhost:8080/api/v1';

  async checkNameAvailability(name: string): Promise<{ available: boolean }> {
    const response = await fetch(
      `${this.baseUrl}/companies/check-name?name=${encodeURIComponent(name)}`
    );
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    
    return response.json();
  }

  async checkNumberAvailability(number: string): Promise<{ available: boolean }> {
    const response = await fetch(
      `${this.baseUrl}/companies/check-number?number=${encodeURIComponent(number)}`
    );
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    
    return response.json();
  }

  async registerCompany(
    companyData: CompanyRegistrationRequest,
    accessToken?: string
  ): Promise<CompanyRegistrationResponse> {
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
    };

    if (accessToken) {
      headers.Authorization = `Bearer ${accessToken}`;
    }

    const response = await fetch(`${this.baseUrl}/companies`, {
      method: 'POST',
      headers,
      body: JSON.stringify(companyData),
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Registration failed: ${response.status} ${errorText}`);
    }

    return response.json();
  }

  /**
   * Generate a unique company number
   * In a real implementation, this would come from the backend
   */
  generateCompanyNumber(): string {
    const timestamp = Date.now().toString().slice(-6);
    const random = Math.floor(Math.random() * 1000).toString().padStart(3, '0');
    return `${timestamp}${random}`;
  }
}

export const registrationService = new RegistrationService();