import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { z } from 'zod';
import { ShareholderService } from '../services/shareholderService';
import { companyService } from '../services/companyService';
import { useAuth } from '../auth/useAuth';
import type { Company, Shareholder, ShareAllocationRequest } from '../types/company';

const shareAllocationSchema = z.object({
  companyId: z.number(),
  shareholderId: z.number(),
  shareClass: z.string().min(1, 'Share class is required'),
  numberOfShares: z.number().min(1, 'Number of shares must be at least 1'),
  nominalValue: z.number().min(0.01, 'Nominal value must be at least 0.01'),
  amountPaid: z.number().min(0, 'Amount paid cannot be negative').optional(),
  allocationDate: z.string().optional(),
  certificateNumber: z.string().optional(),
  restrictions: z.string().optional(),
});

type ShareAllocationFormData = z.infer<typeof shareAllocationSchema>;

interface ShareAllocationProps {
  companyId?: number;
  shareholderId?: number;
}

export const ShareAllocation: React.FC<ShareAllocationProps> = ({ 
  companyId: propCompanyId, 
  shareholderId: propShareholderId 
}) => {
  const { companyId: paramCompanyId, shareholderId: paramShareholderId } = useParams<{ 
    companyId: string; 
    shareholderId: string; 
  }>();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  
  const companyId = propCompanyId || (paramCompanyId ? parseInt(paramCompanyId, 10) : undefined);
  const shareholderId = propShareholderId || (paramShareholderId ? parseInt(paramShareholderId, 10) : undefined);
  
  const [company, setCompany] = useState<Company | null>(null);
  const [shareholders, setShareholders] = useState<Shareholder[]>([]);
  const [selectedShareholder, setSelectedShareholder] = useState<Shareholder | null>(null);
  const [formData, setFormData] = useState<Partial<ShareAllocationFormData>>({
    companyId,
    shareholderId,
    shareClass: 'Ordinary',
    numberOfShares: 1,
    nominalValue: 1.00,
    amountPaid: 0,
    allocationDate: new Date().toISOString().split('T')[0],
    certificateNumber: '',
    restrictions: '',
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const loadCompanyData = useCallback(async () => {
    if (!companyId) return;
    
    setIsLoading(true);
    try {
      const [companyData, shareholdersData] = await Promise.all([
        companyService.getCompanyById(companyId),
        ShareholderService.getShareholdersByCompany(companyId)
      ]);
      
      setCompany(companyData);
      setShareholders(shareholdersData);
    } catch (error) {
      console.error('Error loading company data:', error);
    } finally {
      setIsLoading(false);
    }
  }, [companyId]);

  useEffect(() => {
    if (companyId) {
      loadCompanyData();
    }
  }, [companyId, loadCompanyData]);

  useEffect(() => {
    if (shareholderId && shareholders.length > 0) {
      const shareholder = shareholders.find(s => s.id === shareholderId);
      if (shareholder) {
        setSelectedShareholder(shareholder);
      }
    }
  }, [shareholderId, shareholders]);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    const { name, value, type } = e.target;
    let newValue: string | number = value;
    
    // Convert numeric inputs
    if (type === 'number') {
      newValue = parseFloat(value) || 0;
    }

    setFormData(prev => ({
      ...prev,
      [name]: newValue
    }));

    // Clear error when user starts typing
    if (errors[name]) {
      setErrors(prev => ({
        ...prev,
        [name]: ''
      }));
    }
  };

  const handleShareholderChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const shareholderId = parseInt(e.target.value, 10);
    const shareholder = shareholders.find(s => s.id === shareholderId);
    
    setSelectedShareholder(shareholder || null);
    setFormData(prev => ({
      ...prev,
      shareholderId
    }));
  };

  const calculateTotalValue = (): number => {
    return (formData.numberOfShares || 0) * (formData.nominalValue || 0);
  };

  const calculateUnpaidAmount = (): number => {
    return Math.max(0, calculateTotalValue() - (formData.amountPaid || 0));
  };

  const validateForm = (): boolean => {
    try {
      shareAllocationSchema.parse(formData);
      setErrors({});
      return true;
    } catch (error) {
      if (error instanceof z.ZodError) {
        const newErrors: Record<string, string> = {};
        error.issues.forEach((err) => {
          if (err.path) {
            if (err.path && err.path.length > 0) {
              newErrors[err.path[0] as string] = err.message;
            }
          }
        });
        setErrors(newErrors);
      }
      return false;
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validateForm()) return;

    setIsSubmitting(true);
    try {
      const request: ShareAllocationRequest = {
        companyId: formData.companyId!,
        shareholderId: formData.shareholderId!,
        shareClass: formData.shareClass!,
        numberOfShares: formData.numberOfShares!,
        nominalValue: formData.nominalValue!,
        amountPaid: formData.amountPaid,
        allocationDate: formData.allocationDate,
        certificateNumber: formData.certificateNumber || undefined,
        restrictions: formData.restrictions || undefined,
      };

      await ShareholderService.allocateShares(request);
      
      // Navigate back to company details
      navigate(`/companies/${companyId}`, {
        state: { successMessage: 'Shares allocated successfully' }
      });
    } catch (error) {
      console.error('Error allocating shares:', error);
      setErrors({ general: 'Failed to allocate shares. Please try again.' });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleReset = () => {
    setFormData({
      companyId,
      shareholderId,
      shareClass: 'Ordinary',
      numberOfShares: 1,
      nominalValue: 1.00,
      amountPaid: 0,
      allocationDate: new Date().toISOString().split('T')[0],
      certificateNumber: '',
      restrictions: '',
    });
    setErrors({});
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center min-h-screen">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <div className="max-w-2xl mx-auto p-6">
        <div className="text-center py-8">
          <h2 className="text-2xl font-bold text-gray-900 mb-4">Authentication Required</h2>
          <p className="text-gray-600">Please log in to allocate shares.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto p-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Allocate Shares</h1>
        {company && (
          <p className="text-gray-600">
            For company: <span className="font-semibold">{company.companyName}</span> ({company.companyNumber})
          </p>
        )}
      </div>

      {errors.general && (
        <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-md">
          <p className="text-red-800">{errors.general}</p>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Shareholder Selection */}
        <div className="bg-white shadow-sm rounded-lg p-6 border border-gray-200">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Shareholder Selection</h2>
          
          <div>
            <label htmlFor="shareholderId" className="block text-sm font-medium text-gray-700 mb-1">
              Select Shareholder *
            </label>
            <select
              id="shareholderId"
              name="shareholderId"
              value={formData.shareholderId || ''}
              onChange={handleShareholderChange}
              className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                errors.shareholderId ? 'border-red-500' : 'border-gray-300'
              }`}
              required
            >
              <option value="">Select a shareholder</option>
              {shareholders.map(shareholder => (
                <option key={shareholder.id} value={shareholder.id}>
                  {shareholder.fullName} ({shareholder.isIndividual ? 'Individual' : 'Corporate'})
                </option>
              ))}
            </select>
            {errors.shareholderId && <p className="text-red-500 text-sm mt-1">{errors.shareholderId}</p>}
          </div>

          {selectedShareholder && (
            <div className="mt-4 p-3 bg-gray-50 rounded-md">
              <p className="text-sm text-gray-700">
                <span className="font-semibold">Address:</span> {selectedShareholder.addressLine1}, {selectedShareholder.city}, {selectedShareholder.country}
              </p>
            </div>
          )}
        </div>

        {/* Share Details */}
        <div className="bg-white shadow-sm rounded-lg p-6 border border-gray-200">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Share Details</h2>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label htmlFor="shareClass" className="block text-sm font-medium text-gray-700 mb-1">
                Share Class *
              </label>
              <select
                id="shareClass"
                name="shareClass"
                value={formData.shareClass || ''}
                onChange={handleInputChange}
                className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                  errors.shareClass ? 'border-red-500' : 'border-gray-300'
                }`}
                required
              >
                <option value="Ordinary">Ordinary</option>
                <option value="Preference">Preference</option>
                <option value="Redeemable">Redeemable</option>
                <option value="Voting">Voting</option>
                <option value="Non-Voting">Non-Voting</option>
              </select>
              {errors.shareClass && <p className="text-red-500 text-sm mt-1">{errors.shareClass}</p>}
            </div>

            <div>
              <label htmlFor="numberOfShares" className="block text-sm font-medium text-gray-700 mb-1">
                Number of Shares *
              </label>
              <input
                type="number"
                id="numberOfShares"
                name="numberOfShares"
                value={formData.numberOfShares || ''}
                onChange={handleInputChange}
                className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                  errors.numberOfShares ? 'border-red-500' : 'border-gray-300'
                }`}
                min="1"
                required
              />
              {errors.numberOfShares && <p className="text-red-500 text-sm mt-1">{errors.numberOfShares}</p>}
            </div>

            <div>
              <label htmlFor="nominalValue" className="block text-sm font-medium text-gray-700 mb-1">
                Nominal Value per Share ($) *
              </label>
              <input
                type="number"
                id="nominalValue"
                name="nominalValue"
                value={formData.nominalValue || ''}
                onChange={handleInputChange}
                className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                  errors.nominalValue ? 'border-red-500' : 'border-gray-300'
                }`}
                min="0.01"
                step="0.01"
                required
              />
              {errors.nominalValue && <p className="text-red-500 text-sm mt-1">{errors.nominalValue}</p>}
            </div>

            <div>
              <label htmlFor="amountPaid" className="block text-sm font-medium text-gray-700 mb-1">
                Amount Paid ($)
              </label>
              <input
                type="number"
                id="amountPaid"
                name="amountPaid"
                value={formData.amountPaid || ''}
                onChange={handleInputChange}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                min="0"
                step="0.01"
              />
            </div>

            <div>
              <label htmlFor="allocationDate" className="block text-sm font-medium text-gray-700 mb-1">
                Allocation Date
              </label>
              <input
                type="date"
                id="allocationDate"
                name="allocationDate"
                value={formData.allocationDate || ''}
                onChange={handleInputChange}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label htmlFor="certificateNumber" className="block text-sm font-medium text-gray-700 mb-1">
                Certificate Number
              </label>
              <input
                type="text"
                id="certificateNumber"
                name="certificateNumber"
                value={formData.certificateNumber || ''}
                onChange={handleInputChange}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Optional certificate number"
              />
            </div>
          </div>

          <div className="mt-4">
            <label htmlFor="restrictions" className="block text-sm font-medium text-gray-700 mb-1">
              Restrictions or Notes
            </label>
            <textarea
              id="restrictions"
              name="restrictions"
              value={formData.restrictions || ''}
              onChange={handleInputChange}
              rows={3}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Optional restrictions or notes"
            />
          </div>
        </div>

        {/* Calculation Summary */}
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
          <h3 className="text-lg font-semibold text-blue-900 mb-2">Allocation Summary</h3>
          <div className="grid grid-cols-2 gap-4 text-sm">
            <div>
              <span className="text-blue-700">Total Share Value:</span>
              <span className="font-semibold text-blue-900 ml-2">
                ${calculateTotalValue().toFixed(2)}
              </span>
            </div>
            <div>
              <span className="text-blue-700">Amount Paid:</span>
              <span className="font-semibold text-blue-900 ml-2">
                ${(formData.amountPaid || 0).toFixed(2)}
              </span>
            </div>
            <div>
              <span className="text-blue-700">Unpaid Amount:</span>
              <span className="font-semibold text-blue-900 ml-2">
                ${calculateUnpaidAmount().toFixed(2)}
              </span>
            </div>
            <div>
              <span className="text-blue-700">Fully Paid:</span>
              <span className="font-semibold text-blue-900 ml-2">
                {calculateUnpaidAmount() === 0 ? 'Yes' : 'No'}
              </span>
            </div>
          </div>
        </div>

        {/* Form Actions */}
        <div className="flex justify-end space-x-4">
          <button
            type="button"
            onClick={handleReset}
            className="px-4 py-2 text-gray-700 bg-gray-100 border border-gray-300 rounded-md hover:bg-gray-200 focus:outline-none focus:ring-2 focus:ring-gray-500"
          >
            Reset
          </button>
          <button
            type="submit"
            disabled={isSubmitting}
            className="px-4 py-2 text-white bg-blue-600 border border-transparent rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isSubmitting ? 'Allocating...' : 'Allocate Shares'}
          </button>
        </div>
      </form>
    </div>
  );
};