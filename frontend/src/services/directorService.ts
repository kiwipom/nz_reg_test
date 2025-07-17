import type { DirectorFormData } from '../pages/DirectorAppointment';

interface DirectorAppointmentRequest {
  directorData: Omit<DirectorFormData, 'consentDocument' | 'identificationDocument'>;
  companyId: string;
}

export interface DirectorAppointmentResponse {
  id: number;
  fullName: string;
  firstName?: string;
  lastName?: string;
  status: 'ACTIVE' | 'RESIGNED' | 'DISQUALIFIED';
  appointedDate: string;
  resignedDate?: string;
  consentGiven: boolean;
  consentDate?: string;
  isNzResident: boolean;
  isAustralianResident: boolean;
  residentialCountry: string;
  createdAt: string;
  updatedAt?: string;
}

export class DirectorService {
  private baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

  public getCountryCode(countryName: string): string {
    // Convert full country names to ISO country codes as expected by database
    switch (countryName.toLowerCase()) {
      case 'new zealand':
        return 'NZ';
      case 'australia':
        return 'AU';
      case 'united states':
      case 'united states of america':
        return 'US';
      case 'united kingdom':
        return 'GB';
      case 'canada':
        return 'CA';
      default:
        // Default to NZ if country not recognized
        return 'NZ';
    }
  }

  /**
   * Appoint a new director with optional consent documents
   */
  async appointDirector(
    formData: DirectorFormData, 
    companyId: string, 
    token: string
  ): Promise<DirectorAppointmentResponse> {
    try {
      // TODO: Handle document uploads separately if needed
      // For now, we're only handling the basic director appointment

      // First, fetch the company details to include in the director request
      const companyResponse = await fetch(`${this.baseUrl}/companies/${companyId}`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (!companyResponse.ok) {
        throw new Error('Failed to fetch company details');
      }

      const company = await companyResponse.json();

      // Transform form data to match backend Director entity
      const directorRequest = {
        company: company,
        fullName: `${formData.firstName} ${formData.lastName}`.trim(),
        dateOfBirth: formData.dateOfBirth,
        placeOfBirth: null, // Not captured in form yet
        residentialAddressLine1: formData.residentialAddress.streetAddress,
        residentialAddressLine2: formData.residentialAddress.suburb,
        residentialCity: formData.residentialAddress.city,
        residentialRegion: null, // Not captured in form yet
        residentialPostcode: formData.residentialAddress.postcode,
        residentialCountry: this.getCountryCode(formData.residentialAddress.country),
        isNzResident: formData.residencyStatus === 'NZ_CITIZEN' || formData.residencyStatus === 'NZ_RESIDENT',
        isAustralianResident: formData.residencyStatus === 'AU_CITIZEN' || formData.residencyStatus === 'AU_RESIDENT',
        consentGiven: formData.hasConsentedToAppointment,
        consentDate: formData.hasConsentedToAppointment ? formData.appointmentDate : null,
        appointedDate: formData.appointmentDate,
      };

      const response = await fetch(`${this.baseUrl}/directors`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(directorRequest),
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Director appointment failed: ${response.status} ${errorText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Error appointing director:', error);
      throw error;
    }
  }

  /**
   * Give consent for an existing director
   */
  async giveDirectorConsent(
    directorId: number,
    consentDocument?: File,
    token?: string
  ): Promise<void> {
    try {
      const submitData = new FormData();
      
      if (consentDocument) {
        submitData.append('consentDocument', consentDocument);
      }

      const headers: Record<string, string> = {};
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      const response = await fetch(`${this.baseUrl}/directors/${directorId}/consent`, {
        method: 'POST',
        headers,
        body: consentDocument ? submitData : undefined,
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Consent capture failed: ${response.status} ${errorText}`);
      }
    } catch (error) {
      console.error('Error giving director consent:', error);
      throw error;
    }
  }

  /**
   * Get directors requiring consent for a company
   */
  async getDirectorsRequiringConsent(companyId: string, token?: string): Promise<DirectorAppointmentResponse[]> {
    try {
      const headers: Record<string, string> = {};
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      const response = await fetch(`${this.baseUrl}/companies/${companyId}/directors/requiring-consent`, {
        method: 'GET',
        headers,
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Failed to fetch directors requiring consent: ${response.status} ${errorText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Error fetching directors requiring consent:', error);
      throw error;
    }
  }

  /**
   * Get director details by ID
   */
  async getDirector(directorId: number, token?: string): Promise<DirectorAppointmentResponse> {
    try {
      const headers: Record<string, string> = {};
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      const response = await fetch(`${this.baseUrl}/directors/${directorId}`, {
        method: 'GET',
        headers,
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Failed to fetch director: ${response.status} ${errorText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Error fetching director:', error);
      throw error;
    }
  }

  /**
   * Resign a director
   */
  async resignDirector(
    directorId: number,
    resignationDate?: string,
    token?: string
  ): Promise<DirectorAppointmentResponse> {
    try {
      const headers: Record<string, string> = {};
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      const params = new URLSearchParams();
      if (resignationDate) {
        params.append('resignationDate', resignationDate);
      }

      const response = await fetch(`${this.baseUrl}/directors/${directorId}/resign?${params}`, {
        method: 'POST',
        headers,
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Director resignation failed: ${response.status} ${errorText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Error resigning director:', error);
      throw error;
    }
  }

  /**
   * Get directors for a company
   */
  async getDirectorsByCompany(companyId: string, token?: string): Promise<DirectorAppointmentResponse[]> {
    try {
      const headers: Record<string, string> = {};
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      const response = await fetch(`${this.baseUrl}/directors/company/${companyId}`, {
        method: 'GET',
        headers,
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Failed to fetch directors: ${response.status} ${errorText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Error fetching directors:', error);
      throw error;
    }
  }

  /**
   * Get active directors for a company
   */
  async getActiveDirectorsByCompany(companyId: string, token?: string): Promise<DirectorAppointmentResponse[]> {
    try {
      const headers: Record<string, string> = {};
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      const response = await fetch(`${this.baseUrl}/directors/company/${companyId}/active`, {
        method: 'GET',
        headers,
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Failed to fetch active directors: ${response.status} ${errorText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Error fetching active directors:', error);
      throw error;
    }
  }
}