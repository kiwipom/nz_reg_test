import { api } from './api';
import type { 
  ShareClass, 
  CreateShareClassRequest, 
  UpdateShareClassRequest, 
  ShareClassStatistics 
} from '../types/company';

export class ShareClassService {
  /**
   * Get all active share classes for a company
   */
  static async getActiveShareClassesByCompany(companyId: number): Promise<ShareClass[]> {
    try {
      const response = await api.get(`/v1/companies/${companyId}/share-classes?activeOnly=true`);
      return response.data;
    } catch (error) {
      console.error('Error fetching active share classes:', error);
      throw error;
    }
  }

  /**
   * Get all share classes for a company (including inactive)
   */
  static async getAllShareClassesByCompany(companyId: number): Promise<ShareClass[]> {
    try {
      const response = await api.get(`/v1/companies/${companyId}/share-classes?activeOnly=false`);
      return response.data;
    } catch (error) {
      console.error('Error fetching all share classes:', error);
      throw error;
    }
  }

  /**
   * Get a specific share class by ID
   */
  static async getShareClassById(companyId: number, shareClassId: number): Promise<ShareClass> {
    try {
      const response = await api.get(`/v1/companies/${companyId}/share-classes/${shareClassId}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching share class:', error);
      throw error;
    }
  }

  /**
   * Get share class by company ID and class code
   */
  static async getShareClassByCode(companyId: number, classCode: string): Promise<ShareClass> {
    try {
      const response = await api.get(`/v1/companies/${companyId}/share-classes/by-code/${classCode}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching share class by code:', error);
      throw error;
    }
  }

  /**
   * Create a new share class
   */
  static async createShareClass(companyId: number, request: CreateShareClassRequest): Promise<ShareClass> {
    try {
      const response = await api.post(`/v1/companies/${companyId}/share-classes`, request);
      return response.data;
    } catch (error) {
      console.error('Error creating share class:', error);
      throw error;
    }
  }

  /**
   * Update an existing share class
   */
  static async updateShareClass(
    companyId: number, 
    shareClassId: number, 
    request: UpdateShareClassRequest
  ): Promise<ShareClass> {
    try {
      const response = await api.put(`/v1/companies/${companyId}/share-classes/${shareClassId}`, request);
      return response.data;
    } catch (error) {
      console.error('Error updating share class:', error);
      throw error;
    }
  }

  /**
   * Deactivate a share class (soft delete)
   */
  static async deactivateShareClass(companyId: number, shareClassId: number): Promise<ShareClass> {
    try {
      const response = await api.delete(`/v1/companies/${companyId}/share-classes/${shareClassId}`);
      return response.data;
    } catch (error) {
      console.error('Error deactivating share class:', error);
      throw error;
    }
  }

  /**
   * Get share class statistics for a company
   */
  static async getShareClassStatistics(companyId: number): Promise<ShareClassStatistics[]> {
    try {
      const response = await api.get(`/v1/companies/${companyId}/share-classes/statistics`);
      return response.data;
    } catch (error) {
      console.error('Error fetching share class statistics:', error);
      throw error;
    }
  }

  /**
   * Validate share class data before creation
   */
  static validateCreateRequest(request: CreateShareClassRequest): string[] {
    const errors: string[] = [];

    if (!request.className || request.className.trim().length === 0) {
      errors.push('Class name is required');
    }
    if (request.className && request.className.length > 100) {
      errors.push('Class name cannot exceed 100 characters');
    }

    if (!request.classCode || request.classCode.trim().length === 0) {
      errors.push('Class code is required');
    }
    if (request.classCode && request.classCode.length > 20) {
      errors.push('Class code cannot exceed 20 characters');
    }

    if (request.description && request.description.length > 500) {
      errors.push('Description cannot exceed 500 characters');
    }

    // Voting rights validation
    if (request.votingRights && !['NONE', 'ORDINARY', 'WEIGHTED', 'RESTRICTED'].includes(request.votingRights)) {
      errors.push('Invalid voting rights value');
    }
    if (request.votingRights === 'NONE' && request.votesPerShare && request.votesPerShare > 0) {
      errors.push('Cannot have votes per share when voting rights is NONE');
    }
    if (request.votingRights && request.votingRights !== 'NONE' && (!request.votesPerShare || request.votesPerShare <= 0)) {
      errors.push('Must have positive votes per share when voting rights is not NONE');
    }

    // Dividend rights validation
    if (request.dividendRights && !['NONE', 'ORDINARY', 'PREFERRED', 'CUMULATIVE'].includes(request.dividendRights)) {
      errors.push('Invalid dividend rights value');
    }
    if (request.dividendRights && ['PREFERRED', 'CUMULATIVE'].includes(request.dividendRights) && !request.dividendRate) {
      errors.push('Dividend rate is required for preferred or cumulative dividend rights');
    }
    if (request.dividendRate && (request.dividendRate < 0 || request.dividendRate > 1)) {
      errors.push('Dividend rate must be between 0 and 1 (0% to 100%)');
    }

    // Capital distribution validation
    if (request.capitalDistributionRights && !['ORDINARY', 'PREFERRED', 'NONE'].includes(request.capitalDistributionRights)) {
      errors.push('Invalid capital distribution rights value');
    }

    // Par value validation
    if (request.isNoParValue && request.parValue !== undefined && request.parValue !== null) {
      errors.push('Cannot have par value when shares are designated as no par value');
    }
    if (request.parValue && request.parValue <= 0) {
      errors.push('Par value must be positive');
    }

    // Liquidation preference validation
    if (request.liquidationPreferenceMultiple && request.liquidationPreferenceMultiple <= 0) {
      errors.push('Liquidation preference multiple must be positive');
    }

    // Currency validation
    if (request.currency && request.currency.length !== 3) {
      errors.push('Currency code must be exactly 3 characters');
    }

    return errors;
  }

  /**
   * Format voting rights for display
   */
  static formatVotingRights(shareClass: ShareClass): string {
    switch (shareClass.votingRights) {
      case 'NONE':
        return 'No voting rights';
      case 'ORDINARY':
        return `${shareClass.votesPerShare} vote${shareClass.votesPerShare !== 1 ? 's' : ''} per share`;
      case 'WEIGHTED':
        return `${shareClass.votesPerShare} weighted vote${shareClass.votesPerShare !== 1 ? 's' : ''} per share`;
      case 'RESTRICTED':
        return `${shareClass.votesPerShare} restricted vote${shareClass.votesPerShare !== 1 ? 's' : ''} per share`;
      default:
        return 'Unknown';
    }
  }

  /**
   * Format dividend rights for display
   */
  static formatDividendRights(shareClass: ShareClass): string {
    switch (shareClass.dividendRights) {
      case 'NONE':
        return 'No dividend rights';
      case 'ORDINARY':
        return 'Ordinary dividend rights';
      case 'PREFERRED':
        const rate = shareClass.dividendRate ? (shareClass.dividendRate * 100).toFixed(2) : '0';
        return `${rate}% preferred dividend`;
      case 'CUMULATIVE':
        const cumulativeRate = shareClass.dividendRate ? (shareClass.dividendRate * 100).toFixed(2) : '0';
        return `${cumulativeRate}% cumulative dividend`;
      default:
        return 'Unknown';
    }
  }

  /**
   * Format liquidation preference for display
   */
  static formatLiquidationPreference(shareClass: ShareClass): string {
    if (shareClass.capitalDistributionRights === 'PREFERRED' && shareClass.liquidationPreferenceMultiple) {
      return `${shareClass.liquidationPreferenceMultiple}x liquidation preference`;
    }
    return shareClass.capitalDistributionRights === 'ORDINARY' ? 'Pro rata participation' : 'No liquidation preference';
  }

  /**
   * Get key features summary for a share class
   */
  static getKeyFeatures(shareClass: ShareClass): string[] {
    const features: string[] = [];

    if (shareClass.hasVotingRights) {
      features.push(this.formatVotingRights(shareClass));
    }

    if (shareClass.hasDividendRights) {
      features.push(this.formatDividendRights(shareClass));
    }

    if (shareClass.hasLiquidationPreference) {
      features.push(this.formatLiquidationPreference(shareClass));
    }

    if (shareClass.isRedeemable) {
      features.push('Redeemable');
    }

    if (shareClass.isConvertible) {
      features.push('Convertible');
    }

    if (shareClass.requiresBoardApproval) {
      features.push('Board approval required for transfers');
    }

    if (shareClass.hasPreemptiveRights) {
      features.push('Preemptive rights');
    }

    if (shareClass.hasTagAlongRights) {
      features.push('Tag-along rights');
    }

    if (shareClass.hasDragAlongRights) {
      features.push('Drag-along rights');
    }

    return features.length > 0 ? features : ['Standard ordinary shares'];
  }
}