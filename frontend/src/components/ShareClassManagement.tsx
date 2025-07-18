import { useState, useEffect } from 'react';
import { useAuth0 } from '@auth0/auth0-react';
import { ShareClassService } from '../services/shareClassService';
import type { ShareClass, CreateShareClassRequest } from '../types/company';

interface ShareClassManagementProps {
  companyId: number;
  companyName: string;
  isAuthenticated: boolean;
}

export function ShareClassManagement({ companyId, companyName, isAuthenticated }: ShareClassManagementProps) {
  const { getAccessTokenSilently } = useAuth0();
  const [shareClasses, setShareClasses] = useState<ShareClass[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string>('');
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [isCreating, setIsCreating] = useState(false);

  const [createForm, setCreateForm] = useState<CreateShareClassRequest>({
    className: '',
    classCode: '',
    description: '',
    votingRights: 'ORDINARY',
    votesPerShare: 1,
    dividendRights: 'ORDINARY',
    capitalDistributionRights: 'ORDINARY',
    isTransferable: true,
    hasPreemptiveRights: true
  });

  useEffect(() => {
    loadShareClasses();
  }, [companyId]);

  const loadShareClasses = async () => {
    try {
      setIsLoading(true);
      setError('');
      const classes = await ShareClassService.getActiveShareClassesByCompany(companyId);
      setShareClasses(classes);
    } catch (err) {
      console.error('Error loading share classes:', err);
      setError('Failed to load share classes');
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateShareClass = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!isAuthenticated) return;

    try {
      setIsCreating(true);
      setError('');

      // Validate form
      const validationErrors = ShareClassService.validateCreateRequest(createForm);
      if (validationErrors.length > 0) {
        setError(validationErrors.join(', '));
        return;
      }

      await getAccessTokenSilently();
      const newShareClass = await ShareClassService.createShareClass(companyId, createForm);
      
      setShareClasses(prev => [...prev, newShareClass]);
      setShowCreateForm(false);
      resetCreateForm();
      
    } catch (err: any) {
      console.error('Error creating share class:', err);
      setError(err?.response?.data?.error || 'Failed to create share class');
    } finally {
      setIsCreating(false);
    }
  };

  const resetCreateForm = () => {
    setCreateForm({
      className: '',
      classCode: '',
      description: '',
      votingRights: 'ORDINARY',
      votesPerShare: 1,
      dividendRights: 'ORDINARY',
      capitalDistributionRights: 'ORDINARY',
      isTransferable: true,
      hasPreemptiveRights: true
    });
  };

  const handleDeactivateShareClass = async (shareClassId: number) => {
    if (!isAuthenticated) return;
    
    if (!confirm('Are you sure you want to deactivate this share class? This action cannot be undone.')) {
      return;
    }

    try {
      await getAccessTokenSilently();
      await ShareClassService.deactivateShareClass(companyId, shareClassId);
      setShareClasses(prev => prev.filter(sc => sc.id !== shareClassId));
    } catch (err: any) {
      console.error('Error deactivating share class:', err);
      setError(err?.response?.data?.error || 'Failed to deactivate share class');
    }
  };

  if (isLoading) {
    return (
      <div className="bg-white shadow rounded-lg p-6">
        <div className="animate-pulse">
          <div className="h-6 bg-gray-200 rounded w-1/3 mb-4"></div>
          <div className="space-y-3">
            <div className="h-4 bg-gray-200 rounded w-3/4"></div>
            <div className="h-4 bg-gray-200 rounded w-1/2"></div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white shadow rounded-lg">
      <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
        <h2 className="text-lg font-medium text-gray-900">Share Classes</h2>
        {isAuthenticated && (
          <button
            onClick={() => setShowCreateForm(true)}
            className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-purple-600 hover:bg-purple-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-purple-500"
          >
            <svg className="h-4 w-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
            </svg>
            Create Share Class
          </button>
        )}
      </div>

      <div className="px-6 py-4">
        {error && (
          <div className="mb-4 bg-red-50 border border-red-200 rounded-md p-4">
            <div className="flex">
              <div className="flex-shrink-0">
                <svg className="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
                  <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                </svg>
              </div>
              <div className="ml-3">
                <p className="text-sm text-red-700">{error}</p>
              </div>
            </div>
          </div>
        )}

        {shareClasses.length === 0 ? (
          <div className="text-center py-8">
            <svg className="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
            </svg>
            <h3 className="mt-2 text-sm font-medium text-gray-900">No share classes</h3>
            <p className="mt-1 text-sm text-gray-500">Get started by creating a share class for this company.</p>
            {isAuthenticated && (
              <div className="mt-6">
                <button
                  onClick={() => setShowCreateForm(true)}
                  className="inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-purple-600 hover:bg-purple-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-purple-500"
                >
                  <svg className="-ml-1 mr-2 h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                  </svg>
                  Create Share Class
                </button>
              </div>
            )}
          </div>
        ) : (
          <div className="space-y-4">
            {shareClasses.map((shareClass) => (
              <div key={shareClass.id} className="border border-gray-200 rounded-lg p-4">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      <h3 className="text-lg font-medium text-gray-900">
                        {shareClass.displayName}
                      </h3>
                      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-purple-100 text-purple-800">
                        {shareClass.classCode}
                      </span>
                    </div>
                    
                    {shareClass.description && (
                      <p className="text-sm text-gray-600 mb-3">{shareClass.description}</p>
                    )}
                    
                    <div className="space-y-2">
                      <div className="flex flex-wrap gap-2">
                        {ShareClassService.getKeyFeatures(shareClass).map((feature, index) => (
                          <span key={index} className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
                            {feature}
                          </span>
                        ))}
                      </div>
                      
                      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-3 text-sm">
                        <div>
                          <span className="font-medium text-gray-500">Voting:</span>
                          <p className="text-gray-900">{ShareClassService.formatVotingRights(shareClass)}</p>
                        </div>
                        <div>
                          <span className="font-medium text-gray-500">Dividends:</span>
                          <p className="text-gray-900">{ShareClassService.formatDividendRights(shareClass)}</p>
                        </div>
                        <div>
                          <span className="font-medium text-gray-500">Liquidation:</span>
                          <p className="text-gray-900">{ShareClassService.formatLiquidationPreference(shareClass)}</p>
                        </div>
                      </div>
                    </div>
                  </div>
                  
                  {isAuthenticated && (
                    <div className="ml-4 flex-shrink-0">
                      <button
                        onClick={() => handleDeactivateShareClass(shareClass.id)}
                        className="inline-flex items-center px-3 py-1 border border-red-300 text-sm font-medium rounded-md text-red-700 bg-white hover:bg-red-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500"
                      >
                        Deactivate
                      </button>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Create Share Class Modal */}
        {showCreateForm && (
          <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
            <div className="relative top-20 mx-auto p-5 border w-full max-w-2xl shadow-lg rounded-md bg-white">
              <div className="mt-3">
                <h3 className="text-lg font-medium text-gray-900 mb-4">Create New Share Class</h3>
                
                <form onSubmit={handleCreateShareClass} className="space-y-4">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700">
                        Class Name*
                      </label>
                      <input
                        type="text"
                        required
                        value={createForm.className}
                        onChange={(e) => setCreateForm(prev => ({ ...prev, className: e.target.value }))}
                        className="mt-1 block w-full border border-gray-300 rounded-md px-3 py-2 shadow-sm focus:outline-none focus:ring-purple-500 focus:border-purple-500"
                        placeholder="e.g., Ordinary Shares"
                      />
                    </div>
                    
                    <div>
                      <label className="block text-sm font-medium text-gray-700">
                        Class Code*
                      </label>
                      <input
                        type="text"
                        required
                        value={createForm.classCode}
                        onChange={(e) => setCreateForm(prev => ({ ...prev, classCode: e.target.value.toUpperCase() }))}
                        className="mt-1 block w-full border border-gray-300 rounded-md px-3 py-2 shadow-sm focus:outline-none focus:ring-purple-500 focus:border-purple-500"
                        placeholder="e.g., ORDINARY"
                        maxLength={20}
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700">
                      Description
                    </label>
                    <textarea
                      value={createForm.description || ''}
                      onChange={(e) => setCreateForm(prev => ({ ...prev, description: e.target.value }))}
                      rows={3}
                      className="mt-1 block w-full border border-gray-300 rounded-md px-3 py-2 shadow-sm focus:outline-none focus:ring-purple-500 focus:border-purple-500"
                      placeholder="Optional description of the share class"
                    />
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700">
                        Voting Rights
                      </label>
                      <select
                        value={createForm.votingRights}
                        onChange={(e) => setCreateForm(prev => ({ 
                          ...prev, 
                          votingRights: e.target.value as any,
                          votesPerShare: e.target.value === 'NONE' ? 0 : prev.votesPerShare || 1
                        }))}
                        className="mt-1 block w-full border border-gray-300 rounded-md px-3 py-2 shadow-sm focus:outline-none focus:ring-purple-500 focus:border-purple-500"
                      >
                        <option value="NONE">No voting rights</option>
                        <option value="ORDINARY">Ordinary voting</option>
                        <option value="WEIGHTED">Weighted voting</option>
                        <option value="RESTRICTED">Restricted voting</option>
                      </select>
                    </div>

                    {createForm.votingRights !== 'NONE' && (
                      <div>
                        <label className="block text-sm font-medium text-gray-700">
                          Votes per Share
                        </label>
                        <input
                          type="number"
                          min="1"
                          value={createForm.votesPerShare}
                          onChange={(e) => setCreateForm(prev => ({ ...prev, votesPerShare: parseInt(e.target.value) || 1 }))}
                          className="mt-1 block w-full border border-gray-300 rounded-md px-3 py-2 shadow-sm focus:outline-none focus:ring-purple-500 focus:border-purple-500"
                        />
                      </div>
                    )}
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700">
                        Dividend Rights
                      </label>
                      <select
                        value={createForm.dividendRights}
                        onChange={(e) => setCreateForm(prev => ({ ...prev, dividendRights: e.target.value as any }))}
                        className="mt-1 block w-full border border-gray-300 rounded-md px-3 py-2 shadow-sm focus:outline-none focus:ring-purple-500 focus:border-purple-500"
                      >
                        <option value="NONE">No dividend rights</option>
                        <option value="ORDINARY">Ordinary dividends</option>
                        <option value="PREFERRED">Preferred dividends</option>
                        <option value="CUMULATIVE">Cumulative dividends</option>
                      </select>
                    </div>

                    {createForm.dividendRights && ['PREFERRED', 'CUMULATIVE'].includes(createForm.dividendRights) && (
                      <div>
                        <label className="block text-sm font-medium text-gray-700">
                          Dividend Rate (%)
                        </label>
                        <input
                          type="number"
                          min="0"
                          max="100"
                          step="0.01"
                          value={(createForm.dividendRate || 0) * 100}
                          onChange={(e) => setCreateForm(prev => ({ ...prev, dividendRate: parseFloat(e.target.value) / 100 || 0 }))}
                          className="mt-1 block w-full border border-gray-300 rounded-md px-3 py-2 shadow-sm focus:outline-none focus:ring-purple-500 focus:border-purple-500"
                          placeholder="5.00"
                        />
                      </div>
                    )}
                  </div>

                  <div className="flex flex-wrap gap-4">
                    <label className="flex items-center">
                      <input
                        type="checkbox"
                        checked={createForm.isTransferable}
                        onChange={(e) => setCreateForm(prev => ({ ...prev, isTransferable: e.target.checked }))}
                        className="rounded border-gray-300 text-purple-600 shadow-sm focus:border-purple-300 focus:ring focus:ring-purple-200 focus:ring-opacity-50"
                      />
                      <span className="ml-2 text-sm text-gray-700">Transferable</span>
                    </label>

                    <label className="flex items-center">
                      <input
                        type="checkbox"
                        checked={createForm.hasPreemptiveRights}
                        onChange={(e) => setCreateForm(prev => ({ ...prev, hasPreemptiveRights: e.target.checked }))}
                        className="rounded border-gray-300 text-purple-600 shadow-sm focus:border-purple-300 focus:ring focus:ring-purple-200 focus:ring-opacity-50"
                      />
                      <span className="ml-2 text-sm text-gray-700">Preemptive Rights</span>
                    </label>

                    <label className="flex items-center">
                      <input
                        type="checkbox"
                        checked={createForm.requiresBoardApproval}
                        onChange={(e) => setCreateForm(prev => ({ ...prev, requiresBoardApproval: e.target.checked }))}
                        className="rounded border-gray-300 text-purple-600 shadow-sm focus:border-purple-300 focus:ring focus:ring-purple-200 focus:ring-opacity-50"
                      />
                      <span className="ml-2 text-sm text-gray-700">Requires Board Approval</span>
                    </label>
                  </div>

                  <div className="flex justify-end space-x-3 pt-4">
                    <button
                      type="button"
                      onClick={() => {
                        setShowCreateForm(false);
                        resetCreateForm();
                        setError('');
                      }}
                      className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-purple-500"
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      disabled={isCreating}
                      className="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-purple-600 hover:bg-purple-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-purple-500 disabled:opacity-50"
                    >
                      {isCreating ? 'Creating...' : 'Create Share Class'}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}