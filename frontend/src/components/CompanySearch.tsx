import React, { useState, useEffect } from 'react';
import { Search, Loader2 } from 'lucide-react';
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

  useEffect(() => {
    const delayedSearch = setTimeout(() => {
      if (inputValue.trim()) {
        performSearch(inputValue.trim());
      } else {
        clearSearch();
      }
    }, 300);

    return () => clearTimeout(delayedSearch);
  }, [inputValue]);

  const performSearch = async (query: string) => {
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
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setInputValue(e.target.value);
  };

  const handleClear = () => {
    setInputValue('');
    clearSearch();
  };

  return (
    <div className="w-full max-w-md mx-auto">
      <div className="relative">
        <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
          {isSearching ? (
            <Loader2 className="h-5 w-5 animate-spin text-gray-400" />
          ) : (
            <Search className="h-5 w-5 text-gray-400" />
          )}
        </div>
        
        <input
          type="text"
          value={inputValue}
          onChange={handleInputChange}
          placeholder="Search companies by name or number..."
          className="block w-full pl-10 pr-3 py-2 border border-gray-300 rounded-md leading-5 bg-white placeholder-gray-500 focus:outline-none focus:placeholder-gray-400 focus:ring-1 focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
          aria-label="Search companies"
        />
        
        {inputValue && (
          <button
            onClick={handleClear}
            className="absolute inset-y-0 right-0 pr-3 flex items-center"
            aria-label="Clear search"
          >
            <span className="text-gray-400 hover:text-gray-600 cursor-pointer">
              ×
            </span>
          </button>
        )}
      </div>

      {searchQuery && (
        <div className="mt-2 text-sm text-gray-600">
          {isSearching ? (
            <span>Searching for "{searchQuery}"...</span>
          ) : (
            <span>
              {searchResults.length} result{searchResults.length !== 1 ? 's' : ''} for "{searchQuery}"
            </span>
          )}
        </div>
      )}

      {searchResults.length > 0 && (
        <div className="mt-3 bg-white shadow-lg rounded-md border border-gray-200 max-h-96 overflow-y-auto">
          {searchResults.map((company) => (
            <div
              key={company.id}
              className="p-3 border-b border-gray-100 last:border-b-0 hover:bg-gray-50 cursor-pointer"
              onClick={() => {
                // Handle company selection
                console.log('Selected company:', company);
              }}
            >
              <div className="flex justify-between items-start">
                <div>
                  <h3 className="font-medium text-gray-900">{company.companyName}</h3>
                  <p className="text-sm text-gray-500">
                    {company.companyNumber} • {company.companyType}
                  </p>
                </div>
                <span className={`px-2 py-1 rounded-full text-xs ${
                  company.status === 'ACTIVE' 
                    ? 'bg-green-100 text-green-800' 
                    : 'bg-red-100 text-red-800'
                }`}>
                  {company.status}
                </span>
              </div>
              <p className="text-xs text-gray-400 mt-1">
                Incorporated: {new Date(company.incorporationDate).toLocaleDateString()}
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};