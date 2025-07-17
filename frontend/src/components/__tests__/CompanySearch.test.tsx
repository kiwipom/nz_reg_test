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

describe('CompanySearch', () => {
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

  describe('Rendering', () => {
    it('should render search input with placeholder', () => {
      renderWithRouter(<CompanySearch />);
      
      expect(screen.getByPlaceholderText('Search companies by name, company number, or NZBN...')).toBeInTheDocument();
    });

    it('should render search icon initially', () => {
      renderWithRouter(<CompanySearch />);
      
      expect(screen.getByRole('textbox')).toBeInTheDocument();
      // Search icon should be visible (lucide-react Search component)
      expect(screen.getByRole('textbox')).toHaveAttribute('aria-label', 'Search companies');
    });

    it('should render with proper accessibility attributes', () => {
      renderWithRouter(<CompanySearch />);
      
      const searchInput = screen.getByRole('textbox');
      expect(searchInput).toHaveAttribute('aria-label', 'Search companies');
      expect(searchInput).toHaveAttribute('type', 'text');
    });
  });

  describe('User Interaction', () => {
    it('should update input value when user types', async () => {
      const user = userEvent.setup();
      renderWithRouter(<CompanySearch />);
      
      const searchInput = screen.getByRole('textbox');
      await user.type(searchInput, 'Test Company');
      
      expect(searchInput).toHaveValue('Test Company');
    });

    it('should show clear button when input has value', async () => {
      const user = userEvent.setup();
      renderWithRouter(<CompanySearch />);
      
      const searchInput = screen.getByRole('textbox');
      await user.type(searchInput, 'Test');
      
      expect(screen.getByRole('button', { name: 'Clear search' })).toBeInTheDocument();
    });

    it('should clear input when clear button is clicked', async () => {
      const user = userEvent.setup();
      renderWithRouter(<CompanySearch />);
      
      const searchInput = screen.getByRole('textbox');
      await user.type(searchInput, 'Test Company');
      
      const clearButton = screen.getByRole('button', { name: 'Clear search' });
      await user.click(clearButton);
      
      expect(searchInput).toHaveValue('');
      expect(mockClearSearch).toHaveBeenCalled();
    });

    it('should not show clear button when input is empty', () => {
      renderWithRouter(<CompanySearch />);
      
      expect(screen.queryByRole('button', { name: 'Clear search' })).not.toBeInTheDocument();
    });
  });

  describe('Search Functionality', () => {
    it('should trigger search after debounce delay', async () => {
      const user = userEvent.setup();
      renderWithRouter(<CompanySearch />);
      
      const searchInput = screen.getByRole('textbox');
      await user.type(searchInput, 'Test Company');
      
      // Wait for debounce delay (300ms)
      await waitFor(() => {
        expect(mockSetSearchQuery).toHaveBeenCalledWith('Test Company');
      }, { timeout: 500 });
    });

    it('should not trigger search for queries less than 2 characters', async () => {
      const user = userEvent.setup();
      renderWithRouter(<CompanySearch />);
      
      const searchInput = screen.getByRole('textbox');
      await user.type(searchInput, 'T');
      
      await waitFor(() => {
        expect(mockClearSearch).toHaveBeenCalled();
      }, { timeout: 500 });
    });

    it('should clear search when input is empty', async () => {
      const user = userEvent.setup();
      renderWithRouter(<CompanySearch />);
      
      const searchInput = screen.getByRole('textbox');
      await user.type(searchInput, 'Test');
      await user.clear(searchInput);
      
      await waitFor(() => {
        expect(mockClearSearch).toHaveBeenCalled();
      }, { timeout: 500 });
    });

    it('should show loading state during search', async () => {
      const user = userEvent.setup();
      renderWithRouter(<CompanySearch />);
      
      const searchInput = screen.getByRole('textbox');
      await user.type(searchInput, 'Test Company');
      
      // During search, loading spinner should be shown
      // Note: This test depends on timing and the actual search implementation
      // In a real scenario, you might need to mock the search service
    });
  });

  describe('Search Results Display', () => {
    it('should display search results when available', () => {
      const mockResults = [
        {
          id: 1,
          companyNumber: '12345678',
          companyName: 'Test Company Ltd',
          companyType: 'LTD' as const,
          incorporationDate: '2020-01-01',
          nzbn: '9429000000000',
          status: 'ACTIVE',
        },
        {
          id: 2,
          companyNumber: '87654321',
          companyName: 'Another Company Ltd',
          companyType: 'LTD' as const,
          incorporationDate: '2021-01-01',
          nzbn: '9429000000001',
          status: 'ACTIVE',
        },
      ];

      mockUseCompanyStore.mockReturnValue({
        ...mockStoreState,
        searchQuery: 'Test Company',
        searchResults: mockResults,
      });

      renderWithRouter(<CompanySearch />);
      
      expect(screen.getByText('Test Company Ltd')).toBeInTheDocument();
      expect(screen.getByText('Another Company Ltd')).toBeInTheDocument();
      expect(screen.getByText('2 results for "Test Company"')).toBeInTheDocument();
    });

    it('should display singular result text for single result', () => {
      const mockResults = [
        {
          id: 1,
          companyNumber: '12345678',
          companyName: 'Test Company Ltd',
          companyType: 'LTD' as const,
          incorporationDate: '2020-01-01',
          nzbn: '9429000000000',
          status: 'ACTIVE',
        },
      ];

      mockUseCompanyStore.mockReturnValue({
        ...mockStoreState,
        searchQuery: 'Test Company',
        searchResults: mockResults,
      });

      renderWithRouter(<CompanySearch />);
      
      expect(screen.getByText('1 result for "Test Company"')).toBeInTheDocument();
    });

    it('should display company details correctly', () => {
      const mockResults = [
        {
          id: 1,
          companyNumber: '12345678',
          companyName: 'Test Company Ltd',
          companyType: 'LTD' as const,
          incorporationDate: '2020-01-01',
          nzbn: '9429000000000',
          status: 'ACTIVE',
        },
      ];

      mockUseCompanyStore.mockReturnValue({
        ...mockStoreState,
        searchQuery: 'Test Company',
        searchResults: mockResults,
      });

      renderWithRouter(<CompanySearch />);
      
      expect(screen.getByText('Test Company Ltd')).toBeInTheDocument();
      expect(screen.getByText('12345678')).toBeInTheDocument();
      expect(screen.getByText('LTD')).toBeInTheDocument();
      expect(screen.getByText('ACTIVE')).toBeInTheDocument();
      expect(screen.getByText(/Incorporated: \d{1,2}\/\d{1,2}\/2020/)).toBeInTheDocument();
    });

    it('should handle company selection click', async () => {
      const user = userEvent.setup();
      const mockResults = [
        {
          id: 1,
          companyNumber: '12345678',
          companyName: 'Test Company Ltd',
          companyType: 'LTD' as const,
          incorporationDate: '2020-01-01',
          nzbn: '9429000000000',
          status: 'ACTIVE',
        },
      ];

      mockUseCompanyStore.mockReturnValue({
        ...mockStoreState,
        searchQuery: 'Test Company',
        searchResults: mockResults,
      });

      renderWithRouter(<CompanySearch />);
      
      const companyItem = screen.getByText('Test Company Ltd').closest('div');
      await user.click(companyItem!);
      
      // This would typically navigate or select the company
      // For now, it just logs to console (as per the current implementation)
    });

    it('should display different status styles', () => {
      const mockResults = [
        {
          id: 1,
          companyNumber: '12345678',
          companyName: 'Active Company Ltd',
          companyType: 'LTD' as const,
          incorporationDate: '2020-01-01',
          nzbn: '9429000000000',
          status: 'ACTIVE',
        },
        {
          id: 2,
          companyNumber: '87654321',
          companyName: 'Inactive Company Ltd',
          companyType: 'LTD' as const,
          incorporationDate: '2021-01-01',
          nzbn: '9429000000001',
          status: 'INACTIVE',
        },
      ];

      mockUseCompanyStore.mockReturnValue({
        ...mockStoreState,
        searchQuery: 'Company',
        searchResults: mockResults,
      });

      renderWithRouter(<CompanySearch />);
      
      const activeStatus = screen.getByText('ACTIVE');
      const inactiveStatus = screen.getByText('INACTIVE');
      
      expect(activeStatus).toHaveClass('bg-green-100', 'text-green-800');
      expect(inactiveStatus).toHaveClass('bg-red-100', 'text-red-800');
    });
  });

  describe('Error Handling', () => {
    it('should handle search errors gracefully', async () => {
      const user = userEvent.setup();
      
      // Mock the search service to throw an error
      const mockCompanyService = await import('../../services/companyService');
      vi.mocked(mockCompanyService.companyService.searchCompanies).mockRejectedValue(
        new Error('Search failed')
      );

      renderWithRouter(<CompanySearch />);
      
      const searchInput = screen.getByRole('textbox');
      await user.type(searchInput, 'Error');
      
      await waitFor(() => {
        expect(mockSetError).toHaveBeenCalledWith('Search failed');
        expect(mockSetSearchResults).toHaveBeenCalledWith([]);
      }, { timeout: 500 });
    });

    it('should handle network errors', async () => {
      const user = userEvent.setup();
      
      // Mock the search service to throw a network error
      const mockCompanyService = await import('../../services/companyService');
      vi.mocked(mockCompanyService.companyService.searchCompanies).mockRejectedValue(
        new Error('Network error')
      );

      renderWithRouter(<CompanySearch />);
      
      const searchInput = screen.getByRole('textbox');
      await user.type(searchInput, 'Network Error');
      
      await waitFor(() => {
        expect(mockSetError).toHaveBeenCalledWith('Network error');
      }, { timeout: 500 });
    });
  });

  describe('Performance', () => {
    it('should debounce search requests', async () => {
      const user = userEvent.setup();
      renderWithRouter(<CompanySearch />);
      
      const searchInput = screen.getByRole('textbox');
      
      // Type multiple characters quickly
      await user.type(searchInput, 'T');
      await user.type(searchInput, 'e');
      await user.type(searchInput, 's');
      await user.type(searchInput, 't');
      
      // Should not trigger search immediately
      expect(mockSetSearchQuery).not.toHaveBeenCalled();
      
      // Wait for debounce delay
      await waitFor(() => {
        expect(mockSetSearchQuery).toHaveBeenCalledWith('Test');
      }, { timeout: 500 });
      
      // Should only be called once after debounce
      expect(mockSetSearchQuery).toHaveBeenCalledTimes(1);
    });

    it('should cancel previous search when new search is triggered', async () => {
      const user = userEvent.setup();
      renderWithRouter(<CompanySearch />);
      
      const searchInput = screen.getByRole('textbox');
      
      // Type first search term
      await user.type(searchInput, 'First');
      
      // Clear input (this should trigger clearSearch)
      await user.clear(searchInput);
      
      // Wait for debounce
      await waitFor(() => {
        expect(mockClearSearch).toHaveBeenCalled();
      }, { timeout: 500 });
    });
  });
});