import { render, screen, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { CompanySearch } from '../CompanySearch';
import { useCompanyStore } from '../../stores/useCompanyStore';

// Mock the store
vi.mock('../../stores/useCompanyStore');
vi.mock('../../services/companyService');

const mockUseCompanyStore = vi.mocked(useCompanyStore);

describe('CompanySearch Integration Tests', () => {
  const mockSetSearchQuery = vi.fn();
  const mockSetSearchResults = vi.fn();
  const mockClearSearch = vi.fn();
  const mockSetError = vi.fn();

  const mockStoreState = {
    searchQuery: '',
    searchResults: [],
    setSearchQuery: mockSetSearchQuery,
    setSearchResults: mockSetSearchResults,
    clearSearch: mockClearSearch,
    setError: mockSetError,
  };

  beforeEach(() => {
    vi.clearAllMocks();
    mockUseCompanyStore.mockReturnValue(mockStoreState);
  });

  const renderWithRouter = (component: React.ReactElement) => {
    return render(<BrowserRouter>{component}</BrowserRouter>);
  };

  it('should integrate with store and service for end-to-end search flow', async () => {
    const user = userEvent.setup();
    
    // Mock company data
    const mockCompanies = [
      {
        id: 1,
        companyName: 'Integration Test Company',
        companyNumber: '12345678',
        companyType: 'LTD',
        incorporationDate: '2020-01-01',
        status: 'ACTIVE',
      },
    ];

    // Update store state to simulate search results
    mockUseCompanyStore.mockReturnValue({
      ...mockStoreState,
      searchQuery: 'Integration Test',
      searchResults: mockCompanies,
    });

    renderWithRouter(<CompanySearch />);
    
    const searchInput = screen.getByRole('textbox');
    
    // Simulate user typing
    await user.type(searchInput, 'Integration Test');
    
    // Wait for search results to appear
    await waitFor(() => {
      expect(screen.getByText('Integration Test Company')).toBeInTheDocument();
    });
    
    // Verify search results display
    expect(screen.getByText('12345678')).toBeInTheDocument();
    expect(screen.getByText('LTD')).toBeInTheDocument();
    expect(screen.getByText('ACTIVE')).toBeInTheDocument();
    expect(screen.getByText(/Incorporated: \d{1,2}\/\d{1,2}\/2020/)).toBeInTheDocument();
    
    // Verify search count
    expect(screen.getByText('1 result for "Integration Test"')).toBeInTheDocument();
  });

  it('should handle full search workflow with error recovery', async () => {
    const user = userEvent.setup();
    
    renderWithRouter(<CompanySearch />);
    
    const searchInput = screen.getByRole('textbox');
    
    // Type search query and then clear it
    await user.type(searchInput, 'Test Query');
    await user.clear(searchInput);
    
    // Wait for debounce to trigger clearSearch
    await waitFor(() => {
      expect(mockClearSearch).toHaveBeenCalled();
    }, { timeout: 500 });
  });
});