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

  /**
   * Appoint a new director with optional consent documents
   */
  async appointDirector(
    formData: DirectorFormData, 
    companyId: string, 
    token: string
  ): Promise<DirectorAppointmentResponse> {
    try {
      // Prepare the multipart form data
      const submitData = new FormData();
      
      // Add director data as JSON
      const directorData: DirectorAppointmentRequest = {
        directorData: {
          firstName: formData.firstName,
          lastName: formData.lastName,
          middleName: formData.middleName,
          dateOfBirth: formData.dateOfBirth,
          occupation: formData.occupation,
          nationality: formData.nationality,
          residencyStatus: formData.residencyStatus,
          residentialAddress: formData.residentialAddress,
          serviceAddress: formData.useResidentialForService ? undefined : formData.serviceAddress,
          useResidentialForService: formData.useResidentialForService,
          hasConsentedToAppointment: formData.hasConsentedToAppointment,
          hasReadDirectorDuties: formData.hasReadDirectorDuties,
          isNotDisqualified: formData.isNotDisqualified,
          appointmentDate: formData.appointmentDate,
          directorRole: formData.directorRole,
          hasSigningAuthority: formData.hasSigningAuthority,
        },
        companyId,
      };

      submitData.append('directorData', JSON.stringify(directorData));

      // Add consent document if provided
      if (formData.consentDocument) {
        submitData.append('consentDocument', formData.consentDocument);
      }

      // Add identification document if provided
      if (formData.identificationDocument) {
        submitData.append('identificationDocument', formData.identificationDocument);
      }

      const response = await fetch(`${this.baseUrl}/companies/${companyId}/directors`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          // Note: Don't set Content-Type for FormData, let browser set it with boundary
        },
        body: submitData,
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