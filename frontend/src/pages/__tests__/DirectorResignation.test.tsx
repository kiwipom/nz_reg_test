import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest';
import { DirectorResignation } from '../DirectorResignation';
import { DirectorService } from '../../services/directorService';

// Mock Auth0
const mockGetAccessTokenSilently = vi.fn();
vi.mock('@auth0/auth0-react', () => ({
  useAuth0: () => ({
    getAccessTokenSilently: mockGetAccessTokenSilently,
  }),
}));

// Mock DirectorService
vi.mock('../../services/directorService');

describe('DirectorResignation', () => {
  const mockResignDirector = vi.fn();
  
  beforeEach(() => {
    vi.clearAllMocks();
    mockGetAccessTokenSilently.mockResolvedValue('mock-token');
    
    // Mock the DirectorService constructor and methods
    vi.mocked(DirectorService).mockImplementation(() => ({
      baseUrl: 'http://localhost:8080/api/v1',
      resignDirector: mockResignDirector,
    } as any));
  });

  afterEach(() => {
    vi.clearAllTimers();
  });

  it('renders the director resignation form', () => {
    render(<DirectorResignation />);
    
    expect(screen.getByText('Director Resignation')).toBeInTheDocument();
    expect(screen.getByLabelText('Director ID')).toBeInTheDocument();
    expect(screen.getByLabelText('Resignation Date')).toBeInTheDocument();
    expect(screen.getByLabelText('Confirm resignation')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Resign Director' })).toBeInTheDocument();
  });

  it('pre-fills director ID when provided', () => {
    render(<DirectorResignation directorId={123} directorName="John Doe" />);
    
    const directorIdInput = screen.getByLabelText('Director ID') as HTMLInputElement;
    expect(directorIdInput.value).toBe('123');
    expect(directorIdInput.disabled).toBe(true);
    expect(screen.getByText('Resigning:')).toBeInTheDocument();
    expect(screen.getByText('John Doe')).toBeInTheDocument();
  });

  it('validates required fields', async () => {
    render(<DirectorResignation />);
    
    // Clear the resignation date field to trigger validation
    const resignationDateInput = screen.getByLabelText('Resignation Date');
    fireEvent.change(resignationDateInput, { target: { value: '' } });
    
    const submitButton = screen.getByRole('button', { name: 'Resign Director' });
    fireEvent.click(submitButton);
    
    await waitFor(() => {
      expect(screen.getByText('Director ID is required')).toBeInTheDocument();
      expect(screen.getByText('Resignation date is required')).toBeInTheDocument();
      expect(screen.getByText('You must confirm the resignation')).toBeInTheDocument();
    });
  });

  it('validates confirmation checkbox', async () => {
    render(<DirectorResignation />);
    
    // Fill in required fields except confirmation
    fireEvent.change(screen.getByLabelText('Director ID'), { target: { value: '1' } });
    fireEvent.change(screen.getByLabelText('Resignation Date'), { target: { value: '2024-01-01' } });
    
    const submitButton = screen.getByRole('button', { name: 'Resign Director' });
    fireEvent.click(submitButton);
    
    await waitFor(() => {
      expect(screen.getByText('You must confirm the resignation')).toBeInTheDocument();
    });
  });

  it('submits form successfully', async () => {
    const mockOnComplete = vi.fn();
    mockResignDirector.mockResolvedValue({});
    
    render(<DirectorResignation onResignationComplete={mockOnComplete} />);
    
    // Fill in form
    fireEvent.change(screen.getByLabelText('Director ID'), { target: { value: '1' } });
    fireEvent.change(screen.getByLabelText('Resignation Date'), { target: { value: '2024-01-01' } });
    fireEvent.click(screen.getByLabelText('Confirm resignation'));
    
    const submitButton = screen.getByRole('button', { name: 'Resign Director' });
    fireEvent.click(submitButton);
    
    await waitFor(() => {
      expect(mockResignDirector).toHaveBeenCalledWith(1, '2024-01-01', 'mock-token');
    });
    
    await waitFor(() => {
      expect(screen.getByText('Director resigned successfully')).toBeInTheDocument();
      expect(mockOnComplete).toHaveBeenCalled();
    });
  });

  it('handles submission errors', async () => {
    const errorMessage = 'Cannot resign - company must have at least one director';
    mockResignDirector.mockRejectedValue(new Error(errorMessage));
    
    render(<DirectorResignation />);
    
    // Fill in form
    fireEvent.change(screen.getByLabelText('Director ID'), { target: { value: '1' } });
    fireEvent.change(screen.getByLabelText('Resignation Date'), { target: { value: '2024-01-01' } });
    fireEvent.click(screen.getByLabelText('Confirm resignation'));
    
    const submitButton = screen.getByRole('button', { name: 'Resign Director' });
    fireEvent.click(submitButton);
    
    await waitFor(() => {
      expect(screen.getByText('Resignation failed')).toBeInTheDocument();
      expect(screen.getByText(errorMessage)).toBeInTheDocument();
    });
  });

  it('shows loading state during submission', async () => {
    mockResignDirector.mockImplementation(() => new Promise(resolve => setTimeout(resolve, 100)));
    
    render(<DirectorResignation />);
    
    // Fill in form
    fireEvent.change(screen.getByLabelText('Director ID'), { target: { value: '1' } });
    fireEvent.change(screen.getByLabelText('Resignation Date'), { target: { value: '2024-01-01' } });
    fireEvent.click(screen.getByLabelText('Confirm resignation'));
    
    const submitButton = screen.getByRole('button', { name: 'Resign Director' });
    fireEvent.click(submitButton);
    
    await waitFor(() => {
      expect(screen.getByText('Resigning Director...')).toBeInTheDocument();
      expect(submitButton).toBeDisabled();
    });
  });

  it('resets form when reset button is clicked', async () => {
    render(<DirectorResignation />);
    
    // Fill in form
    fireEvent.change(screen.getByLabelText('Director ID'), { target: { value: '1' } });
    fireEvent.change(screen.getByLabelText('Resignation Date'), { target: { value: '2024-01-01' } });
    fireEvent.click(screen.getByLabelText('Confirm resignation'));
    
    // Click reset
    const resetButton = screen.getByRole('button', { name: 'Reset' });
    fireEvent.click(resetButton);
    
    await waitFor(() => {
      const directorIdInput = screen.getByLabelText('Director ID') as HTMLInputElement;
      expect(directorIdInput.value).toBe('0');
      
      const confirmCheckbox = screen.getByLabelText('Confirm resignation') as HTMLInputElement;
      expect(confirmCheckbox.checked).toBe(false);
    });
  });

  it('displays important notice about resignation requirements', () => {
    render(<DirectorResignation />);
    
    expect(screen.getByText('Important Notice')).toBeInTheDocument();
    expect(screen.getByText(/Ensure the company will still meet the minimum director requirements/)).toBeInTheDocument();
    expect(screen.getByText(/Verify that at least one NZ\/Australian resident director will remain/)).toBeInTheDocument();
    expect(screen.getByText(/This action cannot be undone/)).toBeInTheDocument();
  });
});