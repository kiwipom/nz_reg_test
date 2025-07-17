const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

interface Company {
  id: number;
  companyNumber: string;
  companyName: string;
  companyType: 'LTD' | 'OVERSEAS' | 'UNLIMITED';
  incorporationDate: string;
  nzbn?: string;
  status: string;
}

interface CreateCompanyRequest {
  companyNumber: string;
  companyName: string;
  companyType: 'LTD' | 'OVERSEAS' | 'UNLIMITED';
  incorporationDate: string;
  nzbn?: string;
}

class CompanyService {
  private getTokenFunction: (() => Promise<string>) | null = null;

  setTokenFunction(tokenFunction: () => Promise<string>) {
    this.getTokenFunction = tokenFunction;
  }

  private async fetchWithAuth(url: string, options: RequestInit = {}) {
    let token: string | null = null;
    
    if (this.getTokenFunction) {
      try {
        token = await this.getTokenFunction();
      } catch (error) {
        console.error('Error getting auth token:', error);
        throw new Error('Authentication failed');
      }
    }
    
    const response = await fetch(`${API_BASE_URL}${url}`, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...(token && { Authorization: `Bearer ${token}` }),
        ...options.headers,
      },
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Network error' }));
      throw new Error(error.message || `HTTP ${response.status}`);
    }

    return response.json();
  }

  async searchCompanies(query: string): Promise<Company[]> {
    return this.fetchWithAuth(`/companies/search?query=${encodeURIComponent(query)}`);
  }

  async getCompanyById(id: number): Promise<Company> {
    return this.fetchWithAuth(`/companies/${id}`);
  }

  async getCompanyByNumber(companyNumber: string): Promise<Company> {
    return this.fetchWithAuth(`/companies/number/${companyNumber}`);
  }

  async getActiveCompanies(): Promise<Company[]> {
    return this.fetchWithAuth('/companies');
  }

  async createCompany(company: CreateCompanyRequest): Promise<Company> {
    return this.fetchWithAuth('/companies', {
      method: 'POST',
      body: JSON.stringify(company),
    });
  }

  async updateCompany(id: number, company: Partial<Company>): Promise<Company> {
    return this.fetchWithAuth(`/companies/${id}`, {
      method: 'PUT',
      body: JSON.stringify(company),
    });
  }

  async deleteCompany(id: number): Promise<void> {
    await this.fetchWithAuth(`/companies/${id}`, {
      method: 'DELETE',
    });
  }

  async checkNameAvailability(name: string): Promise<{ available: boolean }> {
    return this.fetchWithAuth(`/companies/check-name?name=${encodeURIComponent(name)}`);
  }

  async checkNumberAvailability(number: string): Promise<{ available: boolean }> {
    return this.fetchWithAuth(`/companies/check-number?number=${encodeURIComponent(number)}`);
  }
}

export const companyService = new CompanyService();
export type { Company, CreateCompanyRequest };