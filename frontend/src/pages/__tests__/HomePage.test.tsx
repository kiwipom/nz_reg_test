import React from 'react';
import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { vi } from 'vitest';
import { HomePage } from '../HomePage';
import { TEST_CONSTANTS } from '../../test/constants';

// Mock the auth hook
vi.mock('../../auth/useAuth', () => ({
  useAuth: vi.fn(() => ({
    isAuthenticated: true,
    user: { roles: ['ADMIN'] },
    loginWithRedirect: vi.fn(),
    logout: vi.fn(),
    getAccessTokenSilently: vi.fn().mockResolvedValue(TEST_CONSTANTS.MOCK_ACCESS_TOKEN),
  })),
}));

// Mock the CompanySearch component
vi.mock('../../components/CompanySearch', () => ({
  CompanySearch: () => <div data-testid="company-search">Company Search Component</div>,
}));

// Mock the ProtectedRoute component
vi.mock('../../components/ProtectedRoute', () => ({
  ProtectedRoute: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

// Mock React Router hooks
const mockUseLocation = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useLocation: () => mockUseLocation(),
  };
});


const renderComponent = (locationState = {}) => {
  mockUseLocation.mockReturnValue({
    state: locationState,
    pathname: '/',
    search: '',
    hash: '',
    key: 'test',
  });

  return render(
    <BrowserRouter>
      <HomePage />
    </BrowserRouter>
  );
};

describe('HomePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the main page content', () => {
    renderComponent();

    expect(screen.getByText('Find Company Information')).toBeInTheDocument();
    expect(screen.getByText(/Search the official New Zealand Companies Register/)).toBeInTheDocument();
    expect(screen.getByTestId('company-search')).toBeInTheDocument();
    expect(screen.getByText('Register New Company')).toBeInTheDocument();
  });

  it('displays success message when provided in location state', () => {
    const successMessage = 'Company "Test Company Ltd" registered successfully! Company Number: 123456789';
    renderComponent({ message: successMessage });

    expect(screen.getByText(successMessage)).toBeInTheDocument();
    expect(screen.getByTestId('check-circle-icon')).toBeInTheDocument();
  });

  it('does not display success message when not provided', () => {
    renderComponent();

    expect(screen.queryByTestId('check-circle-icon')).not.toBeInTheDocument();
    expect(screen.queryByText(/registered successfully/)).not.toBeInTheDocument();
  });

  it('displays success message with proper styling', () => {
    const successMessage = 'Company "Test Company Ltd" registered successfully! Company Number: 123456789';
    renderComponent({ message: successMessage });

    const messageContainer = screen.getByText(successMessage).closest('div');
    expect(messageContainer).toHaveClass('bg-green-50', 'border-green-200');
    expect(screen.getByTestId('check-circle-icon')).toBeInTheDocument();
  });

  it('renders all feature sections', () => {
    renderComponent();

    expect(screen.getByText('Company Details')).toBeInTheDocument();
    expect(screen.getByText('Directors & Shareholders')).toBeInTheDocument();
    expect(screen.getByText('Compliance Records')).toBeInTheDocument();
    expect(screen.getByText('Administrative Features')).toBeInTheDocument();
  });

  it('handles empty success message', () => {
    renderComponent({ message: '' });

    expect(screen.queryByTestId('check-circle-icon')).not.toBeInTheDocument();
  });

  it('handles undefined success message', () => {
    renderComponent({ message: undefined });

    expect(screen.queryByTestId('check-circle-icon')).not.toBeInTheDocument();
  });
});