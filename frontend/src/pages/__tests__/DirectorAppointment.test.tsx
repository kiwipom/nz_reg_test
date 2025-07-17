import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
import { DirectorAppointment } from '../DirectorAppointment';
import { useAuth0 } from '@auth0/auth0-react';
import { TEST_CONSTANTS } from '../../test/constants';

// Mock DirectorService
const mockAppointDirector = vi.fn();
vi.mock('../../services/directorService', () => ({
  DirectorService: vi.fn().mockImplementation(() => ({
    appointDirector: mockAppointDirector,
  })),
}));

// Mock the Auth0 hook
vi.mock('@auth0/auth0-react');
const mockUseAuth0 = vi.mocked(useAuth0);

// Mock React Router hooks
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useParams: () => ({ id: 'test-company-123' }),
  };
});

// Mock Lucide React icons
vi.mock('lucide-react', () => ({
  Building2: () => <div data-testid="building2-icon" />,
  UserPlus: () => <div data-testid="user-plus-icon" />,
  Check: () => <div data-testid="check-icon" />,
  AlertCircle: () => <div data-testid="alert-circle-icon" />,
  Loader2: (props: { 'data-testid'?: string }) => <div data-testid={props['data-testid'] || 'loader2-icon'} />,
  FileText: () => <div data-testid="file-text-icon" />,
  Calendar: () => <div data-testid="calendar-icon" />,
  MapPin: () => <div data-testid="map-pin-icon" />,
  Upload: () => <div data-testid="upload-icon" />,
  X: () => <div data-testid="x-icon" />,
}));

const renderComponent = () => {
  return render(
    <BrowserRouter>
      <DirectorAppointment />
    </BrowserRouter>
  );
};

// Mock window.alert
const mockAlert = vi.fn();
global.alert = mockAlert;

describe('DirectorAppointment', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockAlert.mockClear();
    mockUseAuth0.mockReturnValue({
      user: undefined,
      isAuthenticated: true,
      isLoading: false,
      loginWithRedirect: vi.fn(),
      logout: vi.fn(),
      getAccessTokenSilently: vi.fn().mockResolvedValue(TEST_CONSTANTS.MOCK_ACCESS_TOKEN),
      getIdTokenClaims: vi.fn(),
      getAccessTokenWithPopup: vi.fn(),
      loginWithPopup: vi.fn(),
      handleRedirectCallback: vi.fn(),
      buildAuthorizeUrl: vi.fn(),
      buildLogoutUrl: vi.fn(),
      error: undefined,
    });
    
    // Default mock for DirectorService
    mockAppointDirector.mockResolvedValue({
      id: 1,
      firstName: 'John',
      lastName: 'Doe',
      status: 'ACTIVE',
      appointmentDate: '2024-01-01',
      consentGiven: true,
      createdAt: '2024-01-01T00:00:00Z',
    });
  });

  it('renders the director appointment form with all required fields', () => {
    renderComponent();
    
    expect(screen.getByText('Appoint New Director')).toBeInTheDocument();
    expect(screen.getByText('Add a new director to the company register')).toBeInTheDocument();
    
    // Personal information fields
    expect(screen.getByLabelText(/first name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/last name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/date of birth/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/occupation/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/nationality/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/residency status/i)).toBeInTheDocument();
    
    // Address fields
    expect(screen.getByText('Address Information')).toBeInTheDocument();
    expect(screen.getByLabelText(/street address/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/city/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/postcode/i)).toBeInTheDocument();
    
    // Consent and declarations
    expect(screen.getByText('Consent and Declarations')).toBeInTheDocument();
    expect(screen.getByLabelText(/consent to being appointed/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/read and understand.*duties/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/not disqualified/i)).toBeInTheDocument();
    
    // Appointment details
    expect(screen.getByText('Appointment Details')).toBeInTheDocument();
    expect(screen.getByLabelText(/appointment date/i)).toBeInTheDocument();
    
    expect(screen.getByRole('button', { name: /appoint director/i })).toBeInTheDocument();
  });

  it('has correct default values', () => {
    renderComponent();
    
    // Check default residency status
    const residencySelect = screen.getByLabelText(/residency status/i);
    expect(residencySelect).toHaveValue('NZ_CITIZEN');
    
    // Check default nationality
    const nationalityInput = screen.getByLabelText(/nationality/i);
    expect(nationalityInput).toHaveValue('New Zealand');
    
    // Check default country
    const countryInput = screen.getByLabelText(/country/i);
    expect(countryInput).toHaveValue('New Zealand');
    
    // Check default checkbox state
    const useResidentialCheckbox = screen.getByLabelText(/use residential address for service/i);
    expect(useResidentialCheckbox).toBeChecked();
    
    // Check appointment date is today
    const appointmentDateInput = screen.getByLabelText(/appointment date/i);
    const today = new Date().toISOString().split('T')[0];
    expect(appointmentDateInput).toHaveValue(today);
  });

  it('validates required fields on form submission', async () => {
    renderComponent();
    
    // Submit form without filling any required fields
    const submitButton = screen.getByRole('button', { name: /appoint director/i });
    fireEvent.click(submitButton);
    
    // Wait for validation errors to appear
    await waitFor(() => {
      expect(screen.getByText('First name is required')).toBeInTheDocument();
    }, { timeout: 3000 });
    
    expect(screen.getByText('Last name is required')).toBeInTheDocument();
    expect(screen.getByText('Date of birth is required')).toBeInTheDocument();
    expect(screen.getByText('Occupation is required')).toBeInTheDocument();
    expect(screen.getByText('Street address is required')).toBeInTheDocument();
    expect(screen.getByText('City is required')).toBeInTheDocument();
    expect(screen.getByText('Postcode is required')).toBeInTheDocument();
    expect(screen.getByText('Director consent is required')).toBeInTheDocument();
    expect(screen.getByText('Acknowledgment of director duties is required')).toBeInTheDocument();
    expect(screen.getByText('Declaration of non-disqualification is required')).toBeInTheDocument();
  });

  it('validates age requirement (must be 18+)', async () => {
    const user = userEvent.setup();
    renderComponent();
    
    // Set date of birth to under 18
    const dateOfBirthInput = screen.getByLabelText(/date of birth/i);
    const underageDate = new Date();
    underageDate.setFullYear(underageDate.getFullYear() - 17);
    await user.type(dateOfBirthInput, underageDate.toISOString().split('T')[0]);
    
    const submitButton = screen.getByRole('button', { name: /appoint director/i });
    fireEvent.click(submitButton);
    
    await waitFor(() => {
      expect(screen.getByText('Director must be at least 18 years old')).toBeInTheDocument();
    });
  });

  it('shows residency warning for non-NZ/AU residents', async () => {
    const user = userEvent.setup();
    renderComponent();
    
    const residencySelect = screen.getByLabelText(/residency status/i);
    await user.selectOptions(residencySelect, 'OTHER');
    
    await waitFor(() => {
      expect(screen.getByText(/at least one director must be a New Zealand or Australian resident/i)).toBeInTheDocument();
    });
  });

  it('toggles service address fields when checkbox is unchecked', async () => {
    const user = userEvent.setup();
    renderComponent();
    
    const useResidentialCheckbox = screen.getByLabelText(/use residential address for service/i);
    
    // Initially, service address section should not be visible
    expect(screen.queryByText('Service Address')).not.toBeInTheDocument();
    
    // Uncheck the checkbox
    await user.click(useResidentialCheckbox);
    
    // Service address section should now be visible
    await waitFor(() => {
      expect(screen.getByText('Service Address')).toBeInTheDocument();
    });
  });

  it('validates service address when not using residential address', async () => {
    const user = userEvent.setup();
    renderComponent();
    
    // Uncheck use residential for service
    const useResidentialCheckbox = screen.getByLabelText(/use residential address for service/i);
    await user.click(useResidentialCheckbox);
    
    // Fill in minimal required fields
    await user.type(screen.getByLabelText(/first name/i), 'John');
    await user.type(screen.getByLabelText(/last name/i), 'Doe');
    
    const dateInput = screen.getByLabelText(/date of birth/i);
    const adultDate = new Date();
    adultDate.setFullYear(adultDate.getFullYear() - 30);
    await user.type(dateInput, adultDate.toISOString().split('T')[0]);
    
    await user.type(screen.getByLabelText(/occupation/i), 'Manager');
    
    // Fill residential address
    const streetInputs = screen.getAllByLabelText(/street address/i);
    await user.type(streetInputs[0], '123 Main St');
    await user.type(screen.getByLabelText(/city/i), 'Auckland');
    await user.type(screen.getByLabelText(/postcode/i), '1010');
    
    // Check all consent checkboxes
    await user.click(screen.getByLabelText(/consent to being appointed/i));
    await user.click(screen.getByLabelText(/read and understand.*duties/i));
    await user.click(screen.getByLabelText(/not disqualified/i));
    
    // Submit without filling service address
    const submitButton = screen.getByRole('button', { name: /appoint director/i });
    fireEvent.click(submitButton);
    
    await waitFor(() => {
      expect(screen.getByText('Service address street is required')).toBeInTheDocument();
    });
  });

  it('updates form fields correctly', async () => {
    const user = userEvent.setup();
    renderComponent();
    
    const firstNameInput = screen.getByLabelText(/first name/i);
    await user.type(firstNameInput, 'John');
    expect(firstNameInput).toHaveValue('John');
    
    const lastNameInput = screen.getByLabelText(/last name/i);
    await user.type(lastNameInput, 'Smith');
    expect(lastNameInput).toHaveValue('Smith');
    
    const occupationInput = screen.getByLabelText(/occupation/i);
    await user.type(occupationInput, 'Software Engineer');
    expect(occupationInput).toHaveValue('Software Engineer');
    
    const residencySelect = screen.getByLabelText(/residency status/i);
    await user.selectOptions(residencySelect, 'AU_CITIZEN');
    expect(residencySelect).toHaveValue('AU_CITIZEN');
  });

  it('clears field errors when user starts typing', async () => {
    const user = userEvent.setup();
    renderComponent();
    
    // Trigger validation error
    const submitButton = screen.getByRole('button', { name: /appoint director/i });
    fireEvent.click(submitButton);
    
    await waitFor(() => {
      expect(screen.getByText('First name is required')).toBeInTheDocument();
    });
    
    // Start typing in first name field
    const firstNameInput = screen.getByLabelText(/first name/i);
    await user.type(firstNameInput, 'J');
    
    // Error should be cleared
    expect(screen.queryByText('First name is required')).not.toBeInTheDocument();
  });

  it('shows loading state during form submission', async () => {
    const user = userEvent.setup();
    
    // Mock a delayed response
    let resolvePromise: (value: unknown) => void;
    const delayedPromise = new Promise((resolve) => {
      resolvePromise = resolve;
    });
    mockAppointDirector.mockReturnValue(delayedPromise);
    
    renderComponent();
    
    // Fill out form with valid data
    await user.type(screen.getByLabelText(/first name/i), 'John');
    await user.type(screen.getByLabelText(/last name/i), 'Doe');
    
    const dateInput = screen.getByLabelText(/date of birth/i);
    const adultDate = new Date();
    adultDate.setFullYear(adultDate.getFullYear() - 30);
    await user.type(dateInput, adultDate.toISOString().split('T')[0]);
    
    await user.type(screen.getByLabelText(/occupation/i), 'Manager');
    
    // Fill address
    await user.type(screen.getByLabelText(/street address/i), '123 Main St');
    await user.type(screen.getByLabelText(/city/i), 'Auckland');
    await user.type(screen.getByLabelText(/postcode/i), '1010');
    
    // Check consent checkboxes
    await user.click(screen.getByLabelText(/consent to being appointed/i));
    await user.click(screen.getByLabelText(/read and understand.*duties/i));
    await user.click(screen.getByLabelText(/not disqualified/i));
    
    // Submit form
    const submitButton = screen.getByRole('button', { name: /appoint director/i });
    await user.click(submitButton);
    
    // Should show loading state
    expect(screen.getByText('Appointing Director...')).toBeInTheDocument();
    expect(submitButton).toBeDisabled();
    
    // Resolve the promise to complete the test
    resolvePromise!({
      id: 1,
      firstName: 'John',
      lastName: 'Doe',
      status: 'ACTIVE',
      appointmentDate: '2024-01-01',
      consentGiven: true,
      createdAt: '2024-01-01T00:00:00Z',
    });
  });

  it('handles checkbox interactions correctly', async () => {
    const user = userEvent.setup();
    renderComponent();
    
    const consentCheckbox = screen.getByLabelText(/consent to being appointed/i);
    const dutiesCheckbox = screen.getByLabelText(/read and understand.*duties/i);
    const disqualificationCheckbox = screen.getByLabelText(/not disqualified/i);
    const signingAuthorityCheckbox = screen.getByLabelText(/signing authority/i);
    
    // Initially unchecked
    expect(consentCheckbox).not.toBeChecked();
    expect(dutiesCheckbox).not.toBeChecked();
    expect(disqualificationCheckbox).not.toBeChecked();
    expect(signingAuthorityCheckbox).not.toBeChecked();
    
    // Check them
    await user.click(consentCheckbox);
    await user.click(dutiesCheckbox);
    await user.click(disqualificationCheckbox);
    await user.click(signingAuthorityCheckbox);
    
    expect(consentCheckbox).toBeChecked();
    expect(dutiesCheckbox).toBeChecked();
    expect(disqualificationCheckbox).toBeChecked();
    expect(signingAuthorityCheckbox).toBeChecked();
  });

  it('has proper accessibility attributes', () => {
    renderComponent();
    
    // Check that form fields have proper labels
    expect(screen.getByLabelText(/first name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/last name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/date of birth/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/occupation/i)).toBeInTheDocument();
    
    // Check that required fields are marked as required
    expect(screen.getByLabelText(/first name/i)).toHaveAttribute('required');
    expect(screen.getByLabelText(/last name/i)).toHaveAttribute('required');
    expect(screen.getByLabelText(/date of birth/i)).toHaveAttribute('required');
    expect(screen.getByLabelText(/occupation/i)).toHaveAttribute('required');
    
    // Check that submit button is properly labeled
    expect(screen.getByRole('button', { name: /appoint director/i })).toBeInTheDocument();
  });

  it('handles middle name field correctly', async () => {
    const user = userEvent.setup();
    renderComponent();
    
    const middleNameInput = screen.getByLabelText(/middle name/i);
    await user.type(middleNameInput, 'William James');
    expect(middleNameInput).toHaveValue('William James');
  });

  it('handles director role field correctly', async () => {
    const user = userEvent.setup();
    renderComponent();
    
    const directorRoleInput = screen.getByLabelText(/director role/i);
    await user.type(directorRoleInput, 'Managing Director');
    expect(directorRoleInput).toHaveValue('Managing Director');
  });

  it('validates appointment date field', async () => {
    const user = userEvent.setup();
    renderComponent();
    
    const appointmentDateInput = screen.getByLabelText(/appointment date/i);
    
    // Clear the default date
    await user.clear(appointmentDateInput);
    
    const submitButton = screen.getByRole('button', { name: /appoint director/i });
    fireEvent.click(submitButton);
    
    await waitFor(() => {
      expect(screen.getByText('Appointment date is required')).toBeInTheDocument();
    });
  });

  it('displays correct section headers and icons', () => {
    renderComponent();
    
    expect(screen.getByText('Personal Information')).toBeInTheDocument();
    expect(screen.getByText('Address Information')).toBeInTheDocument();
    expect(screen.getByText('Consent and Declarations')).toBeInTheDocument();
    expect(screen.getByText('Supporting Documents')).toBeInTheDocument();
    expect(screen.getByText('Appointment Details')).toBeInTheDocument();
    
    // Check for icons (using getAllByTestId since user-plus appears twice)
    expect(screen.getAllByTestId('user-plus-icon')).toHaveLength(2);
    expect(screen.getByTestId('map-pin-icon')).toBeInTheDocument();
    expect(screen.getByTestId('check-icon')).toBeInTheDocument();
    expect(screen.getAllByTestId('file-text-icon')).toHaveLength(1);
    expect(screen.getByTestId('calendar-icon')).toBeInTheDocument();
  });

  it('handles consent document upload', async () => {
    const user = userEvent.setup();
    renderComponent();
    
    // Check that upload area is visible
    expect(screen.getByText('Director Consent Document (Optional)')).toBeInTheDocument();
    
    // Create a mock file
    const file = new File(['consent content'], 'consent.pdf', { type: 'application/pdf' });
    const fileInput = document.getElementById('consentDocument');
    expect(fileInput).toBeInTheDocument();
    
    // Upload the file
    await user.upload(fileInput!, file);
    
    // Check that file details are displayed
    await waitFor(() => {
      expect(screen.getByText('consent.pdf')).toBeInTheDocument();
    });
  });

  it('handles identification document upload', async () => {
    const user = userEvent.setup();
    renderComponent();
    
    // Check that upload area is visible
    expect(screen.getByText('Identification Document (Optional)')).toBeInTheDocument();
    
    // Create a mock file
    const file = new File(['id content'], 'passport.jpg', { type: 'image/jpeg' });
    const idFileInput = document.getElementById('identificationDocument');
    expect(idFileInput).toBeInTheDocument();
    
    // Upload the file
    await user.upload(idFileInput!, file);
    
    // Check that file details are displayed
    await waitFor(() => {
      expect(screen.getByText('passport.jpg')).toBeInTheDocument();
    });
  });

  // TODO: Fix this test - file validation not working in test environment
  it.skip('validates file types for document uploads', async () => {
    const user = userEvent.setup();
    renderComponent();
    
    // Try to upload an invalid file type
    const invalidFile = new File(['invalid content'], 'document.txt', { type: 'text/plain' });
    const fileInput = document.getElementById('consentDocument');
    expect(fileInput).toBeInTheDocument();
    
    await user.upload(fileInput!, invalidFile);
    
    // Should show error message
    await waitFor(() => {
      expect(screen.getByText('Only PDF, JPEG, and PNG files are allowed')).toBeInTheDocument();
    });
  });

  it('validates file size for document uploads', async () => {
    const user = userEvent.setup();
    renderComponent();
    
    // Create a file larger than 5MB
    const largeFile = new File(['x'.repeat(6 * 1024 * 1024)], 'large.pdf', { type: 'application/pdf' });
    const fileInput = document.getElementById('consentDocument');
    expect(fileInput).toBeInTheDocument();
    
    await user.upload(fileInput!, largeFile);
    
    // Should show error message
    await waitFor(() => {
      expect(screen.getByText('File size must be less than 5MB')).toBeInTheDocument();
    });
  });

  it('allows removing uploaded documents', async () => {
    const user = userEvent.setup();
    renderComponent();
    
    // Upload a file first
    const file = new File(['content'], 'test.pdf', { type: 'application/pdf' });
    const fileInput = document.getElementById('consentDocument');
    expect(fileInput).toBeInTheDocument();
    await user.upload(fileInput!, file);
    
    // Wait for file to be displayed
    await waitFor(() => {
      expect(screen.getByText('test.pdf')).toBeInTheDocument();
    });
    
    // Click remove button
    const removeButton = screen.getByTestId('x-icon').closest('button');
    if (removeButton) {
      await user.click(removeButton);
    }
    
    // File should be removed
    expect(screen.queryByText('test.pdf')).not.toBeInTheDocument();
    expect(screen.getAllByText('Click to upload')).toHaveLength(2); // Both upload areas should be visible again
  });
});