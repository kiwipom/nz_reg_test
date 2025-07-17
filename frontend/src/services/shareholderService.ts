import { api } from './api';
import type { Shareholder, ShareAllocation, ShareAllocationRequest, ShareTransferRequest, ShareholderPortfolio } from '../types/company';

export class ShareholderService {
  // Shareholder CRUD operations
  static async createShareholder(shareholder: Omit<Shareholder, 'id' | 'createdAt' | 'updatedAt'>): Promise<Shareholder> {
    try {
      const response = await api.post('/v1/shareholders', shareholder);
      return response.data;
    } catch (error) {
      console.error('Error creating shareholder:', error);
      throw error;
    }
  }

  static async getShareholderById(id: number): Promise<Shareholder> {
    try {
      const response = await api.get(`/v1/shareholders/${id}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching shareholder:', error);
      throw error;
    }
  }

  static async updateShareholder(id: number, shareholder: Partial<Shareholder>): Promise<Shareholder> {
    try {
      const response = await api.put(`/v1/shareholders/${id}`, shareholder);
      return response.data;
    } catch (error) {
      console.error('Error updating shareholder:', error);
      throw error;
    }
  }

  static async deleteShareholder(id: number): Promise<void> {
    try {
      await api.delete(`/v1/shareholders/${id}`);
    } catch (error) {
      console.error('Error deleting shareholder:', error);
      throw error;
    }
  }

  // Company shareholders
  static async getShareholdersByCompany(companyId: number): Promise<Shareholder[]> {
    try {
      const response = await api.get(`/v1/shareholders/company/${companyId}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching company shareholders:', error);
      throw error;
    }
  }

  static async getIndividualShareholdersByCompany(companyId: number): Promise<Shareholder[]> {
    try {
      const response = await api.get(`/v1/shareholders/company/${companyId}/individual`);
      return response.data;
    } catch (error) {
      console.error('Error fetching individual shareholders:', error);
      throw error;
    }
  }

  static async getCorporateShareholdersByCompany(companyId: number): Promise<Shareholder[]> {
    try {
      const response = await api.get(`/v1/shareholders/company/${companyId}/corporate`);
      return response.data;
    } catch (error) {
      console.error('Error fetching corporate shareholders:', error);
      throw error;
    }
  }

  // Search and filtering
  static async searchShareholders(query: string): Promise<Shareholder[]> {
    try {
      const response = await api.get('/v1/shareholders/search', {
        params: { query }
      });
      return response.data;
    } catch (error) {
      console.error('Error searching shareholders:', error);
      throw error;
    }
  }

  static async getShareholdersByLocation(companyId: number, city: string, country: string): Promise<Shareholder[]> {
    try {
      const response = await api.get(`/v1/shareholders/company/${companyId}/by-location`, {
        params: { city, country }
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching shareholders by location:', error);
      throw error;
    }
  }

  // Statistics
  static async getShareholderStatistics(companyId: number): Promise<Record<string, unknown>> {
    try {
      const response = await api.get(`/v1/shareholders/company/${companyId}/statistics`);
      return response.data;
    } catch (error) {
      console.error('Error fetching shareholder statistics:', error);
      throw error;
    }
  }

  // Validation
  static async validateShareholderData(id: number): Promise<Record<string, boolean>> {
    try {
      const response = await api.get(`/v1/shareholders/${id}/validate`);
      return response.data;
    } catch (error) {
      console.error('Error validating shareholder data:', error);
      throw error;
    }
  }

  // Share allocation operations
  static async allocateShares(request: ShareAllocationRequest): Promise<ShareAllocation> {
    try {
      const response = await api.post('/v1/share-allocations/allocate', request);
      return response.data;
    } catch (error) {
      console.error('Error allocating shares:', error);
      throw error;
    }
  }

  static async transferShares(allocationId: number, request: ShareTransferRequest): Promise<ShareAllocation> {
    try {
      const response = await api.post(`/v1/share-allocations/${allocationId}/transfer`, request);
      return response.data;
    } catch (error) {
      console.error('Error transferring shares:', error);
      throw error;
    }
  }

  static async updateSharePayment(allocationId: number, additionalPayment: number): Promise<ShareAllocation> {
    try {
      const response = await api.post(`/v1/share-allocations/${allocationId}/payment`, {
        additionalPayment
      });
      return response.data;
    } catch (error) {
      console.error('Error updating share payment:', error);
      throw error;
    }
  }

  static async cancelAllocation(allocationId: number, reason: string): Promise<ShareAllocation> {
    try {
      const response = await api.post(`/v1/share-allocations/${allocationId}/cancel`, {
        reason
      });
      return response.data;
    } catch (error) {
      console.error('Error cancelling allocation:', error);
      throw error;
    }
  }

  // Share allocation queries
  static async getShareAllocation(allocationId: number): Promise<ShareAllocation> {
    try {
      const response = await api.get(`/v1/share-allocations/${allocationId}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching share allocation:', error);
      throw error;
    }
  }

  static async getCompanyAllocations(companyId: number): Promise<ShareAllocation[]> {
    try {
      const response = await api.get(`/v1/share-allocations/company/${companyId}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching company allocations:', error);
      throw error;
    }
  }

  static async getShareholderAllocations(shareholderId: number): Promise<ShareAllocation[]> {
    try {
      const response = await api.get(`/v1/share-allocations/shareholder/${shareholderId}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching shareholder allocations:', error);
      throw error;
    }
  }

  static async getAllocationsByShareClass(companyId: number, shareClass: string): Promise<ShareAllocation[]> {
    try {
      const response = await api.get(`/v1/share-allocations/company/${companyId}/share-class/${shareClass}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching allocations by share class:', error);
      throw error;
    }
  }

  static async getCompanyShareStatistics(companyId: number): Promise<Record<string, unknown>> {
    try {
      const response = await api.get(`/v1/share-allocations/company/${companyId}/statistics`);
      return response.data;
    } catch (error) {
      console.error('Error fetching company share statistics:', error);
      throw error;
    }
  }

  static async getShareholderPortfolio(shareholderId: number): Promise<ShareholderPortfolio> {
    try {
      const response = await api.get(`/v1/share-allocations/shareholder/${shareholderId}/portfolio`);
      return response.data;
    } catch (error) {
      console.error('Error fetching shareholder portfolio:', error);
      throw error;
    }
  }
}