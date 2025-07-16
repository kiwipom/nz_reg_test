import React, { useState, useEffect, useCallback } from 'react';
import { Search, Loader2, Building2, Calendar, CheckCircle, XCircle } from 'lucide-react';
import { useCompanyStore } from '../stores/useCompanyStore';
import { companyService } from '../services/companyService';

export const CompanySearch: React.FC = () => {
  const [inputValue, setInputValue] = useState('');
  const [isSearching, setIsSearching] = useState(false);
  
  const {
    searchQuery,
    searchResults,
    setSearchQuery,
    setSearchResults,
    clearSearch,
    setError
  } = useCompanyStore();

  const performSearch = useCallback(async (query: string) => {
    if (query.length < 2) {
      clearSearch();
      return;
    }

    setIsSearching(true);
    setSearchQuery(query);
    
    try {
      const results = await companyService.searchCompanies(query);
      setSearchResults(results);
      setError(null);
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Search failed');
      setSearchResults([]);
    } finally {
      setIsSearching(false);
    }
  }, [setSearchQuery, setSearchResults, setError, clearSearch]);

  useEffect(() => {
    const delayedSearch = setTimeout(() => {
      if (inputValue.trim()) {
        performSearch(inputValue.trim());
      } else {
        clearSearch();
      }
    }, 300);

    return () => clearTimeout(delayedSearch);
  }, [inputValue, performSearch, clearSearch]);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setInputValue(e.target.value);
  };

  const handleClear = () => {
    setInputValue('');
    clearSearch();
  };

  return (
    <div className="w-full max-w-2xl mx-auto">
      <div className="relative">
        <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
          {isSearching ? (
            <Loader2 className="h-5 w-5 animate-spin text-blue-500" />
          ) : (
            <Search className="h-5 w-5 text-gray-400" />
          )}
        </div>
        
        <input
          type="text"
          value={inputValue}
          onChange={handleInputChange}
          placeholder="Search companies by name, company number, or NZBN..."
          className="block w-full pl-12 pr-12 py-4 border-2 border-gray-200 rounded-xl leading-5 bg-white placeholder-gray-400 focus:outline-none focus:placeholder-gray-300 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-base shadow-sm transition-all duration-200 hover:border-gray-300"
          aria-label="Search companies"
        />
        
        {inputValue && (
          <button
            onClick={handleClear}
            className="absolute inset-y-0 right-0 pr-4 flex items-center"
            aria-label="Clear search"
          >
            <span className="text-gray-400 hover:text-gray-600 cursor-pointer text-2xl font-light">
              ×
            </span>
          </button>
        )}
      </div>

      {searchQuery && (
        <div className="mt-3 text-sm text-gray-600 flex items-center">
          {isSearching ? (
            <div className="flex items-center">
              <Loader2 className="h-4 w-4 animate-spin mr-2" />
              <span>Searching for "{searchQuery}"...</span>
            </div>
          ) : (
            <div className="flex items-center">
              <Search className="h-4 w-4 mr-2 text-gray-400" />
              <span>
                {searchResults.length} result{searchResults.length !== 1 ? 's' : ''} for "{searchQuery}"
              </span>
            </div>
          )}
        </div>
      )}

      {searchResults.length > 0 && (
        <div className="mt-4 bg-white shadow-xl rounded-xl border border-gray-200 max-h-96 overflow-y-auto">
          {searchResults.map((company) => (
            <div
              key={company.id}
              className="p-4 border-b border-gray-100 last:border-b-0 hover:bg-gradient-to-r hover:from-blue-50 hover:to-green-50 cursor-pointer transition-all duration-200 group"
              onClick={() => {
                // Handle company selection
                console.log('Selected company:', company);
              }}
            >
              <div className="flex justify-between items-start">
                <div className="flex-1">
                  <div className="flex items-center mb-2">
                    <div className="flex items-center justify-center w-8 h-8 bg-gradient-to-br from-blue-100 to-green-100 rounded-lg mr-3 group-hover:from-blue-200 group-hover:to-green-200 transition-all duration-200">
                      <Building2 className="h-4 w-4 text-blue-600" />
                    </div>
                    <h3 className="font-semibold text-gray-900 group-hover:text-blue-800 transition-colors duration-200">
                      {company.companyName}
                    </h3>
                  </div>
                  <div className="ml-11">
                    <p className="text-sm text-gray-600 mb-1">
                      <span className="font-medium">Company Number:</span> {company.companyNumber}
                    </p>
                    <p className="text-sm text-gray-600 mb-1">
                      <span className="font-medium">Type:</span> {company.companyType}
                    </p>
                    <div className="flex items-center text-xs text-gray-500 mt-2">
                      <Calendar className="h-3 w-3 mr-1" />
                      <span>Incorporated: {new Date(company.incorporationDate).toLocaleDateString()}</span>
                    </div>
                  </div>
                </div>
                <div className="flex flex-col items-end">
                  <span className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-medium ${
                    company.status === 'ACTIVE' 
                      ? 'bg-green-100 text-green-800 border border-green-200' 
                      : 'bg-red-100 text-red-800 border border-red-200'
                  }`}>
                    {company.status === 'ACTIVE' ? (
                      <CheckCircle className="h-3 w-3 mr-1" />
                    ) : (
                      <XCircle className="h-3 w-3 mr-1" />
                    )}
                    {company.status}
                  </span>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};