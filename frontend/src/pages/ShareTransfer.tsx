import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { z } from 'zod';
import { ShareholderService } from '../services/shareholderService';
import { useAuth } from '../auth/useAuth';
import type { ShareAllocation, Shareholder, ShareTransferRequest } from '../types/company';
import DateInput from '../components/DateInput';

const shareTransferSchema = z.object({
  toShareholderId: z.number().min(1, 'Please select a recipient shareholder'),
  transferDate: z.string().optional(),
  certificateNumber: z.string().optional(),
});

type ShareTransferFormData = z.infer<typeof shareTransferSchema>;

interface ShareTransferProps {
  allocationId?: number;
}

export const ShareTransfer: React.FC<ShareTransferProps> = ({ allocationId: propAllocationId }) => {
  const { allocationId: paramAllocationId } = useParams<{ allocationId: string }>();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  
  const allocationId = propAllocationId || (paramAllocationId ? parseInt(paramAllocationId, 10) : undefined);
  
  const [allocation, setAllocation] = useState<ShareAllocation | null>(null);
  const [shareholders, setShareholders] = useState<Shareholder[]>([]);
  const [selectedShareholder, setSelectedShareholder] = useState<Shareholder | null>(null);
  const [formData, setFormData] = useState<ShareTransferFormData>({
    toShareholderId: 0,
    transferDate: new Date().toISOString().split('T')[0],
    certificateNumber: '',
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const loadAllocationData = useCallback(async () => {
    if (!allocationId) return;
    
    setIsLoading(true);
    try {
      const allocationData = await ShareholderService.getShareAllocation(allocationId);
      setAllocation(allocationData);
      
      // Load shareholders for the same company (excluding current shareholder)
      const shareholdersData = await ShareholderService.getShareholdersByCompany(allocationData.company.id!);
      const availableShareholders = shareholdersData.filter(s => s.id !== allocationData.shareholder.id);
      setShareholders(availableShareholders);
    } catch (error) {
      console.error('Error loading allocation data:', error);
      setErrors({ general: 'Failed to load allocation data. Please try again.' });
    } finally {
      setIsLoading(false);
    }
  }, [allocationId]);

  useEffect(() => {
    if (allocationId) {
      loadAllocationData();
    }
  }, [allocationId, loadAllocationData]);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    
    setFormData(prev => ({
      ...prev,
      [name]: name === 'toShareholderId' ? parseInt(value, 10) : value
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
      toShareholderId: shareholderId
    }));
  };

  const validateForm = (): boolean => {
    try {
      shareTransferSchema.parse(formData);
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
    if (!allocationId) return;

    setIsSubmitting(true);
    try {
      const request: ShareTransferRequest = {
        toShareholderId: formData.toShareholderId,
        transferDate: formData.transferDate,
        certificateNumber: formData.certificateNumber || undefined,
      };

      await ShareholderService.transferShares(allocationId, request);
      
      // Navigate back to company details
      navigate(`/companies/${allocation?.company.id}`, {
        state: { successMessage: 'Shares transferred successfully' }
      });
    } catch (error) {
      console.error('Error transferring shares:', error);
      setErrors({ general: 'Failed to transfer shares. Please try again.' });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCancel = () => {
    navigate(`/companies/${allocation?.company.id}`);
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
          <p className="text-gray-600">Please log in to transfer shares.</p>
        </div>
      </div>
    );
  }

  if (!allocation) {
    return (
      <div className="max-w-2xl mx-auto p-6">
        <div className="text-center py-8">
          <h2 className="text-2xl font-bold text-gray-900 mb-4">Allocation Not Found</h2>
          <p className="text-gray-600">The requested share allocation could not be found.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto p-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Transfer Shares</h1>
        <p className="text-gray-600">
          Company: <span className="font-semibold">{allocation.company.companyName}</span> ({allocation.company.companyNumber})
        </p>
      </div>

      {errors.general && (
        <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-md">
          <p className="text-red-800">{errors.general}</p>
        </div>
      )}

      {/* Current Allocation Details */}
      <div className="bg-white shadow-sm rounded-lg p-6 border border-gray-200 mb-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Current Allocation Details</h2>
        
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <span className="text-sm text-gray-500">Current Shareholder:</span>
            <p className="font-semibold text-gray-900">{allocation.shareholder.fullName}</p>
          </div>
          <div>
            <span className="text-sm text-gray-500">Share Class:</span>
            <p className="font-semibold text-gray-900">{allocation.shareClass}</p>
          </div>
          <div>
            <span className="text-sm text-gray-500">Number of Shares:</span>
            <p className="font-semibold text-gray-900">{allocation.numberOfShares.toLocaleString()}</p>
          </div>
          <div>
            <span className="text-sm text-gray-500">Nominal Value per Share:</span>
            <p className="font-semibold text-gray-900">${allocation.nominalValue.toFixed(2)}</p>
          </div>
          <div>
            <span className="text-sm text-gray-500">Total Value:</span>
            <p className="font-semibold text-gray-900">
              ${(allocation.numberOfShares * allocation.nominalValue).toFixed(2)}
            </p>
          </div>
          <div>
            <span className="text-sm text-gray-500">Amount Paid:</span>
            <p className="font-semibold text-gray-900">${allocation.amountPaid.toFixed(2)}</p>
          </div>
          {allocation.certificateNumber && (
            <div>
              <span className="text-sm text-gray-500">Certificate Number:</span>
              <p className="font-semibold text-gray-900">{allocation.certificateNumber}</p>
            </div>
          )}
          <div>
            <span className="text-sm text-gray-500">Fully Paid:</span>
            <p className="font-semibold text-gray-900">{allocation.isFullyPaid ? 'Yes' : 'No'}</p>
          </div>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Transfer Details */}
        <div className="bg-white shadow-sm rounded-lg p-6 border border-gray-200">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Transfer Details</h2>
          
          <div className="space-y-4">
            <div>
              <label htmlFor="toShareholderId" className="block text-sm font-medium text-gray-700 mb-1">
                Transfer to Shareholder *
              </label>
              <select
                id="toShareholderId"
                name="toShareholderId"
                value={formData.toShareholderId || ''}
                onChange={handleShareholderChange}
                className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                  errors.toShareholderId ? 'border-red-500' : 'border-gray-300'
                }`}
                required
              >
                <option value="">Select recipient shareholder</option>
                {shareholders.map(shareholder => (
                  <option key={shareholder.id} value={shareholder.id}>
                    {shareholder.fullName} ({shareholder.isIndividual ? 'Individual' : 'Corporate'})
                  </option>
                ))}
              </select>
              {errors.toShareholderId && <p className="text-red-500 text-sm mt-1">{errors.toShareholderId}</p>}
            </div>

            {selectedShareholder && (
              <div className="p-3 bg-gray-50 rounded-md">
                <p className="text-sm text-gray-700">
                  <span className="font-semibold">Recipient Address:</span> {selectedShareholder.addressLine1}, {selectedShareholder.city}, {selectedShareholder.country}
                </p>
                <p className="text-sm text-gray-700 mt-1">
                  <span className="font-semibold">Type:</span> {selectedShareholder.isIndividual ? 'Individual' : 'Corporate Entity'}
                </p>
              </div>
            )}

            <div>
              <DateInput
                id="transferDate"
                name="transferDate"
                value={formData.transferDate || ''}
                onChange={(value) => handleInputChange({ target: { name: 'transferDate', value } } as React.ChangeEvent<HTMLInputElement>)}
                label="Transfer Date"
              />
            </div>

            <div>
              <label htmlFor="certificateNumber" className="block text-sm font-medium text-gray-700 mb-1">
                New Certificate Number
              </label>
              <input
                type="text"
                id="certificateNumber"
                name="certificateNumber"
                value={formData.certificateNumber || ''}
                onChange={handleInputChange}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Optional new certificate number"
              />
            </div>
          </div>
        </div>

        {/* Warning Notice */}
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
          <div className="flex">
            <div className="flex-shrink-0">
              <svg className="h-5 w-5 text-yellow-400" viewBox="0 0 20 20" fill="currentColor">
                <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
              </svg>
            </div>
            <div className="ml-3">
              <h3 className="text-sm font-medium text-yellow-800">Important Notice</h3>
              <p className="mt-1 text-sm text-yellow-700">
                This transfer will permanently move the shares to the selected shareholder. The original allocation will be marked as transferred and cannot be reversed.
              </p>
            </div>
          </div>
        </div>

        {/* Form Actions */}
        <div className="flex justify-end space-x-4">
          <button
            type="button"
            onClick={handleCancel}
            className="px-4 py-2 text-gray-700 bg-gray-100 border border-gray-300 rounded-md hover:bg-gray-200 focus:outline-none focus:ring-2 focus:ring-gray-500"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={isSubmitting}
            className="px-4 py-2 text-white bg-blue-600 border border-transparent rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isSubmitting ? 'Transferring...' : 'Transfer Shares'}
          </button>
        </div>
      </form>
    </div>
  );
};