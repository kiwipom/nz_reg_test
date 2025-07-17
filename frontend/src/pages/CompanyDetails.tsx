import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth0 } from '@auth0/auth0-react';
import { companyService, type Company } from '../services/companyService';
import { DirectorService, type DirectorAppointmentResponse } from '../services/directorService';

interface CompanyDetailsProps {
  companyId?: string;
}

export function CompanyDetails({ companyId: propCompanyId }: CompanyDetailsProps) {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { getAccessTokenSilently, isAuthenticated } = useAuth0();
  
  const companyId = propCompanyId || id;
  
  const [company, setCompany] = useState<Company | null>(null);
  const [directors, setDirectors] = useState<DirectorAppointmentResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string>('');
  
  const loadCompanyData = useCallback(async () => {
    setIsLoading(true);
    setError('');

    try {
      let token: string | undefined;
      if (isAuthenticated) {
        token = await getAccessTokenSilently();
        companyService.setTokenFunction(() => getAccessTokenSilently());
      }

      // Load company details
      const companyData = await companyService.getCompanyById(Number(companyId));
      setCompany(companyData);

      // Load directors
      const directorService = new DirectorService();
      const directorsData = await directorService.getDirectorsByCompany(companyId!, token);
      setDirectors(directorsData);

    } catch (err) {
      console.error('Error loading company data:', err);
      setError(err instanceof Error ? err.message : 'Failed to load company data');
    } finally {
      setIsLoading(false);
    }
  }, [companyId, isAuthenticated, getAccessTokenSilently]);

  useEffect(() => {
    if (!companyId) {
      setError('Company ID is required');
      setIsLoading(false);
      return;
    }

    loadCompanyData();
  }, [companyId, loadCompanyData]);

  const handleAppointDirector = () => {
    navigate(`/companies/${companyId}/directors/appoint`);
  };

  const handleResignDirector = (directorId: number, directorName: string) => {
    navigate(`/companies/${companyId}/directors/${directorId}/resign`, {
      state: { directorName }
    });
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-NZ');
  };

  const getDirectorStatusBadge = (status: string) => {
    const baseClasses = "inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium";
    
    switch (status) {
      case 'ACTIVE':
        return `${baseClasses} bg-green-100 text-green-800`;
      case 'RESIGNED':
        return `${baseClasses} bg-gray-100 text-gray-800`;
      case 'DISQUALIFIED':
        return `${baseClasses} bg-red-100 text-red-800`;
      default:
        return `${baseClasses} bg-gray-100 text-gray-800`;
    }
  };

  const getResidencyStatus = (director: DirectorAppointmentResponse) => {
    if (director.isNzResident) return 'NZ Resident';
    if (director.isAustralianResident) return 'Australian Resident';
    return `Resident of ${director.residentialCountry}`;
  };

  if (isLoading) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="animate-pulse">
          <div className="h-8 bg-gray-200 rounded w-1/3 mb-6"></div>
          <div className="space-y-4">
            <div className="h-4 bg-gray-200 rounded w-1/4"></div>
            <div className="h-4 bg-gray-200 rounded w-1/2"></div>
            <div className="h-4 bg-gray-200 rounded w-1/3"></div>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="bg-red-50 border border-red-200 rounded-md p-4">
          <div className="flex">
            <div className="flex-shrink-0">
              <svg className="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
              </svg>
            </div>
            <div className="ml-3">
              <h3 className="text-sm font-medium text-red-800">Error</h3>
              <p className="text-sm text-red-700 mt-1">{error}</p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (!company) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <p className="text-gray-500">Company not found</p>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Company Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">{company.companyName}</h1>
        <p className="text-lg text-gray-600">Company Number: {company.companyNumber}</p>
      </div>

      {/* Company Details Card */}
      <div className="bg-white shadow rounded-lg mb-8">
        <div className="px-6 py-4 border-b border-gray-200">
          <h2 className="text-lg font-medium text-gray-900">Company Information</h2>
        </div>
        <div className="px-6 py-4">
          <dl className="grid grid-cols-1 gap-x-4 gap-y-6 sm:grid-cols-2">
            <div>
              <dt className="text-sm font-medium text-gray-500">Company Type</dt>
              <dd className="mt-1 text-sm text-gray-900">{company.companyType}</dd>
            </div>
            <div>
              <dt className="text-sm font-medium text-gray-500">Status</dt>
              <dd className="mt-1">
                <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                  company.status === 'ACTIVE' ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                }`}>
                  {company.status}
                </span>
              </dd>
            </div>
            <div>
              <dt className="text-sm font-medium text-gray-500">Incorporation Date</dt>
              <dd className="mt-1 text-sm text-gray-900">{formatDate(company.incorporationDate)}</dd>
            </div>
            {company.nzbn && (
              <div>
                <dt className="text-sm font-medium text-gray-500">NZBN</dt>
                <dd className="mt-1 text-sm text-gray-900">{company.nzbn}</dd>
              </div>
            )}
          </dl>
        </div>
      </div>

      {/* Directors Section */}
      <div className="bg-white shadow rounded-lg">
        <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
          <h2 className="text-lg font-medium text-gray-900">Directors</h2>
          {isAuthenticated && (
            <button
              onClick={handleAppointDirector}
              className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            >
              <svg className="h-4 w-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
              </svg>
              Appoint Director
            </button>
          )}
        </div>
        <div className="px-6 py-4">
          {directors.length === 0 ? (
            <p className="text-gray-500 text-center py-8">No directors found for this company.</p>
          ) : (
            <div className="space-y-4">
              {directors.map((director) => (
                <div key={director.id} className="border border-gray-200 rounded-lg p-4">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <h3 className="text-lg font-medium text-gray-900">
                        {director.fullName || `${director.firstName} ${director.lastName}`}
                      </h3>
                      <div className="mt-2 space-y-1">
                        <p className="text-sm text-gray-600">
                          <span className="font-medium">Status:</span>{' '}
                          <span className={getDirectorStatusBadge(director.status)}>
                            {director.status}
                          </span>
                        </p>
                        <p className="text-sm text-gray-600">
                          <span className="font-medium">Appointed:</span>{' '}
                          {formatDate(director.appointmentDate)}
                        </p>
                        {director.resignationDate && (
                          <p className="text-sm text-gray-600">
                            <span className="font-medium">Resigned:</span>{' '}
                            {formatDate(director.resignationDate)}
                          </p>
                        )}
                        <p className="text-sm text-gray-600">
                          <span className="font-medium">Residency:</span>{' '}
                          {getResidencyStatus(director)}
                        </p>
                        <p className="text-sm text-gray-600">
                          <span className="font-medium">Consent Given:</span>{' '}
                          <span className={director.consentGiven ? 'text-green-600' : 'text-red-600'}>
                            {director.consentGiven ? 'Yes' : 'No'}
                          </span>
                          {director.consentDate && (
                            <span className="text-gray-500">
                              {' '}({formatDate(director.consentDate)})
                            </span>
                          )}
                        </p>
                      </div>
                    </div>
                    {isAuthenticated && director.status === 'ACTIVE' && (
                      <div className="ml-4">
                        <button
                          onClick={() => handleResignDirector(
                            director.id, 
                            director.fullName || `${director.firstName} ${director.lastName}`
                          )}
                          className="inline-flex items-center px-3 py-1 border border-red-300 text-sm font-medium rounded-md text-red-700 bg-white hover:bg-red-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500"
                        >
                          Resign
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}