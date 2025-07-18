import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
import { CompanyRegistration } from '../CompanyRegistration';
import { useAuth } from '../../auth/useAuth';
import { registrationService } from '../../services/registrationService';
import { TEST_CONSTANTS } from '../../test/constants';

// Mock the auth hook
vi.mock('../../auth/useAuth');
const mockUseAuth = vi.mocked(useAuth);

// Mock the registration service
vi.mock('../../services/registrationService', () => ({
  registrationService: {
    checkNameAvailability: vi.fn(),
    registerCompany: vi.fn(),
    generateCompanyNumber: vi.fn(),
  },
}));

// Mock React Router hooks
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});


const renderComponent = () => {
  return render(
    <BrowserRouter>
      <CompanyRegistration />
    </BrowserRouter>
  );
};


// Mock fetch
const mockFetch = vi.fn();
global.fetch = mockFetch;

// Mock window.alert
const mockAlert = vi.fn();
global.alert = mockAlert;

describe('CompanyRegistration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockFetch.mockClear();
    mockAlert.mockClear();
    mockUseAuth.mockReturnValue({
      user: undefined,
      isAuthenticated: true,
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      getAccessToken: vi.fn().mockResolvedValue(TEST_CONSTANTS.MOCK_ACCESS_TOKEN),
      getUserRoles: vi.fn().mockResolvedValue([]),
      hasRole: vi.fn().mockResolvedValue(false),
      hasAnyRole: vi.fn().mockResolvedValue(false),
    });
  });

  it('renders the registration form with all required fields', () => {
    renderComponent();
    
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
    renderComponent();
    
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
    renderComponent();
    
    // Submit form without filling any required fields
    const form = document.querySelector('form');
    if (form) {
      fireEvent.submit(form);
    } else {
      // Fallback to button click
      const submitButton = screen.getByRole('button', { name: /register company/i });
      fireEvent.click(submitButton);
    }
    
    await waitFor(() => {
      expect(screen.getByText('Company name is required')).toBeInTheDocument();
      expect(screen.getByText('Incorporation date is required')).toBeInTheDocument();
    });
  });

  it('validates share capital calculation', async () => {
    const user = userEvent.setup();
    renderComponent();
    
    // Set company name to enable form submission
    const companyNameInput = screen.getByLabelText(/company name/i);
    await user.type(companyNameInput, 'Test Company');
    
    // Set share capital to 200, but keep numberOfShares (100) and shareValue (1) as defaults
    const shareCapitalInput = screen.getByLabelText(/share capital/i);
    fireEvent.change(shareCapitalInput, { target: { name: 'shareCapital', value: '200' } });
    
    const incorporationDateInput = screen.getByLabelText(/incorporation date/i);
    await user.type(incorporationDateInput, '01/01/2024');
    
    const form = document.querySelector('form');
    if (form) {
      fireEvent.submit(form);
    }
    
    await waitFor(() => {
      expect(screen.getByText('Share capital must equal number of shares × share value')).toBeInTheDocument();
    });
  });

  it('validates positive values for share-related fields', async () => {
    const user = userEvent.setup();
    renderComponent();
    
    // Set company name to enable form submission
    const companyNameInput = screen.getByLabelText(/company name/i);
    await user.type(companyNameInput, 'Test Company');
    
    const shareCapitalInput = screen.getByLabelText(/share capital/i);
    fireEvent.change(shareCapitalInput, { target: { name: 'shareCapital', value: '0' } });
    
    const numberOfSharesInput = screen.getByLabelText(/number of shares/i);
    fireEvent.change(numberOfSharesInput, { target: { name: 'numberOfShares', value: '0' } });
    
    const shareValueInput = screen.getByLabelText(/value per share/i);
    fireEvent.change(shareValueInput, { target: { name: 'shareValue', value: '0' } });
    
    const form = document.querySelector('form');
    if (form) {
      fireEvent.submit(form);
    }
    
    await waitFor(() => {
      expect(screen.getByText('Share capital must be greater than 0')).toBeInTheDocument();
      expect(screen.getByText('Number of shares must be greater than 0')).toBeInTheDocument();
      expect(screen.getByText('Value per share must be greater than 0')).toBeInTheDocument();
    });
  });

  it('checks name availability when typing company name', async () => {
    const user = userEvent.setup();
    const mockCheckNameAvailability = vi.mocked(registrationService.checkNameAvailability);
    mockCheckNameAvailability.mockResolvedValue({ available: true });
    
    renderComponent();
    
    const companyNameInput = screen.getByLabelText(/company name/i);
    
    // Type the company name
    await user.type(companyNameInput, 'Test Company Ltd');
    
    // Wait for the debounce timeout (500ms) plus buffer
    await new Promise(resolve => setTimeout(resolve, 700));
    
    // Wait for the API call to have been made
    await waitFor(() => {
      expect(mockCheckNameAvailability).toHaveBeenCalledWith('Test Company Ltd');
    }, { timeout: 2000 });
    
    // Wait for the state update to complete and UI to update
    await waitFor(() => {
      expect(screen.getByText('✓ Name is available')).toBeInTheDocument();
    }, { timeout: 2000 });
  });

  it('shows error when company name is not available', async () => {
    const user = userEvent.setup();
    const mockCheckNameAvailability = vi.mocked(registrationService.checkNameAvailability);
    mockCheckNameAvailability.mockResolvedValue({ available: false });
    
    renderComponent();
    
    const companyNameInput = screen.getByLabelText(/company name/i);
    await user.type(companyNameInput, 'Existing Company');
    
    // Wait for the debounce timeout (500ms) plus buffer
    await new Promise(resolve => setTimeout(resolve, 700));
    
    // Wait for debounced API call
    await waitFor(() => {
      expect(mockCheckNameAvailability).toHaveBeenCalledWith('Existing Company');
    }, { timeout: 2000 });
    
    // Try to submit
    const incorporationDateInput = screen.getByLabelText(/incorporation date/i);
    await user.type(incorporationDateInput, '01/01/2024');
    
    const form = document.querySelector('form');
    if (form) {
      fireEvent.submit(form);
    }
    
    await waitFor(() => {
      expect(screen.getByText('Company name is not available')).toBeInTheDocument();
    });
  });

  it('shows loading state while checking name availability', async () => {
    const user = userEvent.setup();
    
    // Mock a delayed response
    const mockCheckNameAvailability = vi.mocked(registrationService.checkNameAvailability);
    mockCheckNameAvailability.mockImplementationOnce(() => 
      new Promise(resolve => setTimeout(() => resolve({ available: true }), 1000))
    );
    
    renderComponent();
    
    const companyNameInput = screen.getByLabelText(/company name/i);
    await user.type(companyNameInput, 'Test Company');
    
    // Wait for debounce to complete and API call to start
    await new Promise(resolve => setTimeout(resolve, 700));
    
    // Should show loading state after debounce (500ms) but before API response (1000ms)
    await waitFor(() => {
      expect(screen.getByTestId('loading-spinner')).toBeInTheDocument();
    }, { timeout: 500 });
  });

  it('handles API errors gracefully', async () => {
    const user = userEvent.setup();
    const mockCheckNameAvailability = vi.mocked(registrationService.checkNameAvailability);
    mockCheckNameAvailability.mockRejectedValueOnce(new Error('Network error'));
    
    // Mock console.error to avoid test noise
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    
    renderComponent();
    
    const companyNameInput = screen.getByLabelText(/company name/i);
    await user.type(companyNameInput, 'Test Company');
    
    // Wait for the debounce timeout (500ms) plus buffer
    await new Promise(resolve => setTimeout(resolve, 700));
    
    // Wait for debounced API call
    await waitFor(() => {
      expect(mockCheckNameAvailability).toHaveBeenCalled();
    }, { timeout: 2000 });
    
    // Should not show availability status on error
    await waitFor(() => {
      expect(screen.queryByText('✓ Name is available')).not.toBeInTheDocument();
    }, { timeout: 2000 });
    
    consoleSpy.mockRestore();
  });

  it('updates form fields correctly', async () => {
    const user = userEvent.setup();
    renderComponent();
    
    const companyNameInput = screen.getByLabelText(/company name/i);
    await user.type(companyNameInput, 'Test Company');
    expect(companyNameInput).toHaveValue('Test Company');
    
    const companyTypeSelect = screen.getByLabelText(/company type/i);
    await user.selectOptions(companyTypeSelect, 'OVERSEAS');
    expect(companyTypeSelect).toHaveValue('OVERSEAS');
    
    const incorporationDateInput = screen.getByLabelText(/incorporation date/i);
    await user.type(incorporationDateInput, '01/01/2024');
    expect(incorporationDateInput).toHaveValue('01/01/2024');
    
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
    renderComponent();
    
    // Trigger validation error
    const form = document.querySelector('form');
    if (form) {
      fireEvent.submit(form);
    }
    
    await waitFor(() => {
      expect(screen.getByText('Company name is required')).toBeInTheDocument();
    });
    
    // Start typing in company name field
    const companyNameInput = screen.getByLabelText(/company name/i);
    await user.type(companyNameInput, 'T');
    
    // Error should be cleared
    expect(screen.queryByText('Company name is required')).not.toBeInTheDocument();
  });

  it('disables submit button when name is not available', async () => {
    const user = userEvent.setup();
    const mockCheckNameAvailability = vi.mocked(registrationService.checkNameAvailability);
    mockCheckNameAvailability.mockResolvedValue({ available: false });
    
    renderComponent();
    
    const companyNameInput = screen.getByLabelText(/company name/i);
    await user.type(companyNameInput, 'Existing Company');
    
    // Wait for the debounce timeout (500ms) plus buffer
    await new Promise(resolve => setTimeout(resolve, 700));
    
    // Wait for debounced API call
    await waitFor(() => {
      expect(mockCheckNameAvailability).toHaveBeenCalled();
    }, { timeout: 2000 });
    
    const submitButton = screen.getByRole('button', { name: /register company/i });
    
    await waitFor(() => {
      expect(submitButton).toBeDisabled();
    }, { timeout: 2000 });
  });

  it('shows loading state during form submission', async () => {
    const user = userEvent.setup();
    
    // Mock name availability check
    const mockCheckNameAvailability = vi.mocked(registrationService.checkNameAvailability);
    mockCheckNameAvailability.mockResolvedValue({ available: true });
    
    // Mock registration service with delay
    const mockRegisterCompany = vi.mocked(registrationService.registerCompany);
    mockRegisterCompany.mockImplementationOnce(() => 
      new Promise(resolve => setTimeout(() => resolve({
        id: 1,
        companyNumber: '123456789',
        companyName: 'Test Company',
        companyType: 'LTD',
        incorporationDate: '2024-01-01',
        status: 'ACTIVE',
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1,
      }), 1000))
    );
    
    const mockGenerateCompanyNumber = vi.mocked(registrationService.generateCompanyNumber);
    mockGenerateCompanyNumber.mockReturnValue('123456789');
    
    renderComponent();
    
    // Fill out form
    const companyNameInput = screen.getByLabelText(/company name/i);
    await user.type(companyNameInput, 'Test Company');
    
    const incorporationDateInput = screen.getByLabelText(/incorporation date/i);
    await user.type(incorporationDateInput, '01/01/2024');
    
    // Wait for the debounce timeout (500ms) plus buffer
    await new Promise(resolve => setTimeout(resolve, 700));
    
    // Wait for name availability check
    await waitFor(() => {
      expect(screen.getByText('✓ Name is available')).toBeInTheDocument();
    }, { timeout: 2000 });
    
    // Submit form
    const submitButton = screen.getByRole('button', { name: /register company/i });
    await user.click(submitButton);
    
    // Should show loading state
    expect(screen.getByText('Registering Company...')).toBeInTheDocument();
    expect(submitButton).toBeDisabled();
  });

  it('has proper accessibility attributes', () => {
    renderComponent();
    
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

  describe('File Upload Functionality', () => {
    it('renders file upload area with correct styling', () => {
      renderComponent();
      
      expect(screen.getByText('Constitution Document (Optional)')).toBeInTheDocument();
      expect(screen.getByText('Upload the company constitution document (PDF, DOC, or DOCX format, max 5MB)')).toBeInTheDocument();
      expect(screen.getByText('Click to upload or drag and drop')).toBeInTheDocument();
      expect(screen.getByText('PDF, DOC, DOCX (max 5MB)')).toBeInTheDocument();
      expect(screen.getByTestId('upload-icon')).toBeInTheDocument();
    });

    it('accepts valid PDF file', async () => {
      renderComponent();
      
      const fileInput = screen.getByLabelText('Constitution Document (Optional)');
      const file = new File(['test content'], 'constitution.pdf', { type: 'application/pdf' });
      
      fireEvent.change(fileInput, { target: { files: [file] } });
      
      await waitFor(() => {
        expect(screen.getByText('constitution.pdf')).toBeInTheDocument();
        expect(screen.getByText('0.00 MB')).toBeInTheDocument();
        expect(screen.getByTestId('file-text-icon')).toBeInTheDocument();
      });
    });

    it('accepts valid DOC file', async () => {
      renderComponent();
      
      const fileInput = screen.getByLabelText('Constitution Document (Optional)');
      const file = new File(['test content'], 'constitution.doc', { type: 'application/msword' });
      
      fireEvent.change(fileInput, { target: { files: [file] } });
      
      await waitFor(() => {
        expect(screen.getByText('constitution.doc')).toBeInTheDocument();
      });
    });

    it('accepts valid DOCX file', async () => {
      renderComponent();
      
      const fileInput = screen.getByLabelText('Constitution Document (Optional)');
      const file = new File(['test content'], 'constitution.docx', { 
        type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' 
      });
      
      fireEvent.change(fileInput, { target: { files: [file] } });
      
      await waitFor(() => {
        expect(screen.getByText('constitution.docx')).toBeInTheDocument();
      });
    });

    it('rejects invalid file type', async () => {
      renderComponent();
      
      const fileInput = screen.getByLabelText('Constitution Document (Optional)');
      const file = new File(['test content'], 'constitution.txt', { type: 'text/plain' });
      
      fireEvent.change(fileInput, { target: { files: [file] } });
      
      await waitFor(() => {
        expect(screen.getByText('Only PDF, DOC, and DOCX files are allowed')).toBeInTheDocument();
      });
    });

    it('rejects file larger than 5MB', async () => {
      renderComponent();
      
      const fileInput = screen.getByLabelText('Constitution Document (Optional)');
      const largeContent = 'x'.repeat(6 * 1024 * 1024); // 6MB
      const file = new File([largeContent], 'constitution.pdf', { type: 'application/pdf' });
      
      fireEvent.change(fileInput, { target: { files: [file] } });
      
      await waitFor(() => {
        expect(screen.getByText('File size must be less than 5MB')).toBeInTheDocument();
      });
    });

    it('allows removing uploaded file', async () => {
      renderComponent();
      
      const fileInput = screen.getByLabelText('Constitution Document (Optional)');
      const file = new File(['test content'], 'constitution.pdf', { type: 'application/pdf' });
      
      fireEvent.change(fileInput, { target: { files: [file] } });
      
      await waitFor(() => {
        expect(screen.getByText('constitution.pdf')).toBeInTheDocument();
      });
      
      const removeButton = screen.getByRole('button', { name: '' });
      fireEvent.click(removeButton);
      
      await waitFor(() => {
        expect(screen.queryByText('constitution.pdf')).not.toBeInTheDocument();
        expect(screen.getByText('Click to upload or drag and drop')).toBeInTheDocument();
      });
    });

    it('clears file error when valid file is uploaded', async () => {
      renderComponent();
      
      const fileInput = screen.getByLabelText('Constitution Document (Optional)');
      
      // First upload invalid file
      const invalidFile = new File(['test content'], 'constitution.txt', { type: 'text/plain' });
      fireEvent.change(fileInput, { target: { files: [invalidFile] } });
      
      await waitFor(() => {
        expect(screen.getByText('Only PDF, DOC, and DOCX files are allowed')).toBeInTheDocument();
      });
      
      // Then upload valid file
      const validFile = new File(['test content'], 'constitution.pdf', { type: 'application/pdf' });
      fireEvent.change(fileInput, { target: { files: [validFile] } });
      
      await waitFor(() => {
        expect(screen.queryByText('Only PDF, DOC, and DOCX files are allowed')).not.toBeInTheDocument();
        expect(screen.getByText('constitution.pdf')).toBeInTheDocument();
      });
    });

    it('displays file size in MB correctly', async () => {
      renderComponent();
      
      const fileInput = screen.getByLabelText('Constitution Document (Optional)');
      const content = 'x'.repeat(2 * 1024 * 1024); // 2MB
      const file = new File([content], 'constitution.pdf', { type: 'application/pdf' });
      
      fireEvent.change(fileInput, { target: { files: [file] } });
      
      await waitFor(() => {
        expect(screen.getByText('2.00 MB')).toBeInTheDocument();
      });
    });

    it('handles file upload without selecting file', async () => {
      renderComponent();
      
      const fileInput = screen.getByLabelText('Constitution Document (Optional)');
      fireEvent.change(fileInput, { target: { files: [] } });
      
      // Should not show any file or error
      expect(screen.queryByTestId('file-text-icon')).not.toBeInTheDocument();
      expect(screen.getByText('Click to upload or drag and drop')).toBeInTheDocument();
    });

    it('shows upload area when no file is selected', () => {
      renderComponent();
      
      expect(screen.getByText('Click to upload or drag and drop')).toBeInTheDocument();
      expect(screen.getByTestId('upload-icon')).toBeInTheDocument();
      expect(screen.queryByTestId('file-text-icon')).not.toBeInTheDocument();
    });

    it('shows file details when file is selected', async () => {
      renderComponent();
      
      const fileInput = screen.getByLabelText('Constitution Document (Optional)');
      const file = new File(['test content'], 'constitution.pdf', { type: 'application/pdf' });
      
      fireEvent.change(fileInput, { target: { files: [file] } });
      
      await waitFor(() => {
        expect(screen.queryByText('Click to upload or drag and drop')).not.toBeInTheDocument();
        expect(screen.queryByTestId('upload-icon')).not.toBeInTheDocument();
        expect(screen.getByTestId('file-text-icon')).toBeInTheDocument();
        expect(screen.getByText('constitution.pdf')).toBeInTheDocument();
        expect(screen.getByTestId('x-icon')).toBeInTheDocument();
      });
    });
  });

  describe('Form Integration with File Upload', () => {
    it('submits form successfully with constitution file', async () => {
      const user = userEvent.setup();
      const mockCheckNameAvailability = vi.mocked(registrationService.checkNameAvailability);
      const mockRegisterCompany = vi.mocked(registrationService.registerCompany);
      const mockGenerateCompanyNumber = vi.mocked(registrationService.generateCompanyNumber);
      
      mockCheckNameAvailability.mockResolvedValue({ available: true });
      mockRegisterCompany.mockResolvedValue({
        id: 1,
        companyNumber: '123456789',
        companyName: 'Test Company Ltd',
        companyType: 'LTD',
        incorporationDate: '2024-01-01',
        status: 'ACTIVE',
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1,
      });
      mockGenerateCompanyNumber.mockReturnValue('123456789');
      
      renderComponent();
      
      // Fill out the form
      const companyNameInput = screen.getByLabelText('Company Name *');
      await user.type(companyNameInput, 'Test Company Ltd');
      
      // Wait for name availability check
      await waitFor(() => {
        expect(screen.getByText('✓ Name is available')).toBeInTheDocument();
      });
      
      const incorporationDateInput = screen.getByLabelText('Incorporation Date *');
      await user.type(incorporationDateInput, '01/01/2024');
      
      // Upload constitution file
      const fileInput = screen.getByLabelText('Constitution Document (Optional)');
      const file = new File(['test content'], 'constitution.pdf', { type: 'application/pdf' });
      fireEvent.change(fileInput, { target: { files: [file] } });
      
      await waitFor(() => {
        expect(screen.getByText('constitution.pdf')).toBeInTheDocument();
      });
      
      // Submit the form
      const submitButton = screen.getByRole('button', { name: /register company/i });
      await user.click(submitButton);
      
      await waitFor(() => {
        expect(mockRegisterCompany).toHaveBeenCalledWith(
          expect.objectContaining({
            companyNumber: '123456789',
            companyName: 'Test Company Ltd',
            companyType: 'LTD',
            incorporationDate: '2024-01-01',
          }),
          TEST_CONSTANTS.MOCK_ACCESS_TOKEN
        );
        expect(mockNavigate).toHaveBeenCalledWith('/', {
          state: {
            message: 'Company "Test Company Ltd" registered successfully! Company Number: 123456789'
          }
        });
      });
    });

    it('maintains file selection during form validation', async () => {
      renderComponent();
      
      // Upload a valid file
      const fileInput = screen.getByLabelText('Constitution Document (Optional)');
      const file = new File(['test content'], 'constitution.pdf', { type: 'application/pdf' });
      fireEvent.change(fileInput, { target: { files: [file] } });
      
      await waitFor(() => {
        expect(screen.getByText('constitution.pdf')).toBeInTheDocument();
      });
      
      // Try to submit form with invalid data (should trigger validation)
      const form = document.querySelector('form');
      if (form) {
        fireEvent.submit(form);
      }
      
      // File should still be selected
      expect(screen.getByText('constitution.pdf')).toBeInTheDocument();
    });
  });
});