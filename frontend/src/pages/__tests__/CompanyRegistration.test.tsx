import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { CompanyRegistration } from '../CompanyRegistration';

// Mock fetch
const mockFetch = vi.fn();
global.fetch = mockFetch;

describe('CompanyRegistration', () => {
  beforeEach(() => {
    mockFetch.mockClear();
  });

  it('renders the registration form with all required fields', () => {
    render(<CompanyRegistration />);
    
    expect(screen.getByText('Register a New Company')).toBeInTheDocument();
    expect(screen.getByLabelText(/company name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/company type/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/incorporation date/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/nzbn/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/share capital/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/number of shares/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/value per share/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /register company/i })).toBeInTheDocument();
  });

  it('has correct default values', () => {
    render(<CompanyRegistration />);
    
    // Check select element value
    const companyTypeSelect = screen.getByLabelText(/company type/i);
    expect(companyTypeSelect).toHaveValue('LTD');
    
    // Check number input values
    const shareCapitalInput = screen.getByLabelText(/share capital/i);
    expect(shareCapitalInput).toHaveValue(100);
    
    const numberOfSharesInput = screen.getByLabelText(/number of shares/i);
    expect(numberOfSharesInput).toHaveValue(100);
    
    const shareValueInput = screen.getByLabelText(/value per share/i);
    expect(shareValueInput).toHaveValue(1);
  });

  it('validates required fields', async () => {
    const user = userEvent.setup();
    render(<CompanyRegistration />);
    
    const submitButton = screen.getByRole('button', { name: /register company/i });
    await user.click(submitButton);
    
    expect(screen.getByText('Company name is required')).toBeInTheDocument();
    expect(screen.getByText('Incorporation date is required')).toBeInTheDocument();
  });

  it('validates share capital calculation', async () => {
    const user = userEvent.setup();
    render(<CompanyRegistration />);
    
    // Set share capital to 200, but keep numberOfShares (100) and shareValue (1) as defaults
    const shareCapitalInput = screen.getByLabelText(/share capital/i);
    await user.clear(shareCapitalInput);
    await user.type(shareCapitalInput, '200');
    
    const companyNameInput = screen.getByLabelText(/company name/i);
    await user.type(companyNameInput, 'Test Company');
    
    const incorporationDateInput = screen.getByLabelText(/incorporation date/i);
    await user.type(incorporationDateInput, '2024-01-01');
    
    const submitButton = screen.getByRole('button', { name: /register company/i });
    await user.click(submitButton);
    
    expect(screen.getByText('Share capital must equal number of shares × share value')).toBeInTheDocument();
  });

  it('validates positive values for share-related fields', async () => {
    const user = userEvent.setup();
    render(<CompanyRegistration />);
    
    const shareCapitalInput = screen.getByLabelText(/share capital/i);
    await user.clear(shareCapitalInput);
    await user.type(shareCapitalInput, '0');
    
    const numberOfSharesInput = screen.getByLabelText(/number of shares/i);
    await user.clear(numberOfSharesInput);
    await user.type(numberOfSharesInput, '0');
    
    const shareValueInput = screen.getByLabelText(/value per share/i);
    await user.clear(shareValueInput);
    await user.type(shareValueInput, '0');
    
    const submitButton = screen.getByRole('button', { name: /register company/i });
    await user.click(submitButton);
    
    expect(screen.getByText('Share capital must be greater than 0')).toBeInTheDocument();
    expect(screen.getByText('Number of shares must be greater than 0')).toBeInTheDocument();
    expect(screen.getByText('Value per share must be greater than 0')).toBeInTheDocument();
  });

  it('checks name availability when typing company name', async () => {
    const user = userEvent.setup();
    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ available: true })
    });
    
    render(<CompanyRegistration />);
    
    const companyNameInput = screen.getByLabelText(/company name/i);
    await user.type(companyNameInput, 'Test Company Ltd');
    
    // Wait for debounced API call
    await waitFor(() => {
      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/companies/check-name?name=Test%20Company%20Ltd'
      );
    });
    
    // Check that the availability indicator shows
    await waitFor(() => {
      expect(screen.getByText('✓ Name is available')).toBeInTheDocument();
    });
  });

  it('shows error when company name is not available', async () => {
    const user = userEvent.setup();
    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ available: false })
    });
    
    render(<CompanyRegistration />);
    
    const companyNameInput = screen.getByLabelText(/company name/i);
    await user.type(companyNameInput, 'Existing Company');
    
    // Wait for debounced API call
    await waitFor(() => {
      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/companies/check-name?name=Existing%20Company'
      );
    });
    
    // Try to submit
    const incorporationDateInput = screen.getByLabelText(/incorporation date/i);
    await user.type(incorporationDateInput, '2024-01-01');
    
    const submitButton = screen.getByRole('button', { name: /register company/i });
    await user.click(submitButton);
    
    expect(screen.getByText('Company name is not available')).toBeInTheDocument();
  });

  it('shows loading state while checking name availability', async () => {
    const user = userEvent.setup();
    
    // Mock a delayed response
    mockFetch.mockImplementationOnce(() => 
      new Promise(resolve => setTimeout(() => resolve({
        ok: true,
        json: async () => ({ available: true })
      }), 100))
    );
    
    render(<CompanyRegistration />);
    
    const companyNameInput = screen.getByLabelText(/company name/i);
    await user.type(companyNameInput, 'Test Company');
    
    // Should show loading state
    await waitFor(() => {
      expect(screen.getByTestId('loading-spinner')).toBeInTheDocument();
    }, { timeout: 100 });
  });

  it('handles API errors gracefully', async () => {
    const user = userEvent.setup();
    mockFetch.mockRejectedValueOnce(new Error('Network error'));
    
    // Mock console.error to avoid test noise
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    
    render(<CompanyRegistration />);
    
    const companyNameInput = screen.getByLabelText(/company name/i);
    await user.type(companyNameInput, 'Test Company');
    
    // Wait for debounced API call
    await waitFor(() => {
      expect(mockFetch).toHaveBeenCalled();
    });
    
    // Should not show availability status on error
    await waitFor(() => {
      expect(screen.queryByText('✓ Name is available')).not.toBeInTheDocument();
    });
    
    consoleSpy.mockRestore();
  });

  it('updates form fields correctly', async () => {
    const user = userEvent.setup();
    render(<CompanyRegistration />);
    
    const companyNameInput = screen.getByLabelText(/company name/i);
    await user.type(companyNameInput, 'Test Company');
    expect(companyNameInput).toHaveValue('Test Company');
    
    const companyTypeSelect = screen.getByLabelText(/company type/i);
    await user.selectOptions(companyTypeSelect, 'OVERSEAS');
    expect(companyTypeSelect).toHaveValue('OVERSEAS');
    
    const incorporationDateInput = screen.getByLabelText(/incorporation date/i);
    await user.type(incorporationDateInput, '2024-01-01');
    expect(incorporationDateInput).toHaveValue('2024-01-01');
    
    const nzbnInput = screen.getByLabelText(/nzbn/i);
    await user.type(nzbnInput, '1234567890123');
    expect(nzbnInput).toHaveValue('1234567890123');
    
    const shareCapitalInput = screen.getByLabelText(/share capital/i);
    await user.clear(shareCapitalInput);
    await user.type(shareCapitalInput, '500');
    expect(shareCapitalInput).toHaveValue(500);
    
    const numberOfSharesInput = screen.getByLabelText(/number of shares/i);
    await user.clear(numberOfSharesInput);
    await user.type(numberOfSharesInput, '250');
    expect(numberOfSharesInput).toHaveValue(250);
    
    const shareValueInput = screen.getByLabelText(/value per share/i);
    await user.clear(shareValueInput);
    await user.type(shareValueInput, '2');
    expect(shareValueInput).toHaveValue(2);
  });

  it('clears field errors when user starts typing', async () => {
    const user = userEvent.setup();
    render(<CompanyRegistration />);
    
    // Trigger validation error
    const submitButton = screen.getByRole('button', { name: /register company/i });
    await user.click(submitButton);
    
    expect(screen.getByText('Company name is required')).toBeInTheDocument();
    
    // Start typing in company name field
    const companyNameInput = screen.getByLabelText(/company name/i);
    await user.type(companyNameInput, 'T');
    
    // Error should be cleared
    expect(screen.queryByText('Company name is required')).not.toBeInTheDocument();
  });

  it('disables submit button when name is not available', async () => {
    const user = userEvent.setup();
    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ available: false })
    });
    
    render(<CompanyRegistration />);
    
    const companyNameInput = screen.getByLabelText(/company name/i);
    await user.type(companyNameInput, 'Existing Company');
    
    await waitFor(() => {
      expect(mockFetch).toHaveBeenCalled();
    });
    
    const submitButton = screen.getByRole('button', { name: /register company/i });
    
    await waitFor(() => {
      expect(submitButton).toBeDisabled();
    });
  });

  it('shows loading state during form submission', async () => {
    const user = userEvent.setup();
    
    // Mock name availability check
    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ available: true })
    });
    
    render(<CompanyRegistration />);
    
    // Fill out form
    const companyNameInput = screen.getByLabelText(/company name/i);
    await user.type(companyNameInput, 'Test Company');
    
    const incorporationDateInput = screen.getByLabelText(/incorporation date/i);
    await user.type(incorporationDateInput, '2024-01-01');
    
    // Wait for name availability check
    await waitFor(() => {
      expect(screen.getByText('✓ Name is available')).toBeInTheDocument();
    });
    
    // Submit form
    const submitButton = screen.getByRole('button', { name: /register company/i });
    await user.click(submitButton);
    
    // Should show loading state
    expect(screen.getByText('Registering Company...')).toBeInTheDocument();
    expect(submitButton).toBeDisabled();
  });

  it('has proper accessibility attributes', () => {
    render(<CompanyRegistration />);
    
    // Check that form fields have proper labels
    expect(screen.getByLabelText(/company name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/company type/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/incorporation date/i)).toBeInTheDocument();
    
    // Check that required fields are marked as required
    expect(screen.getByLabelText(/company name/i)).toHaveAttribute('required');
    expect(screen.getByLabelText(/incorporation date/i)).toHaveAttribute('required');
    
    // Check that submit button is properly labeled
    expect(screen.getByRole('button', { name: /register company/i })).toBeInTheDocument();
  });
});