import '@testing-library/jest-dom';
import React from 'react';
import { beforeAll, afterEach, afterAll, vi } from 'vitest';
import { cleanup } from '@testing-library/react';
import { server } from './mocks/server';
import { TEST_CONSTANTS } from './constants';

// Setup MSW
beforeAll(() => {
  server.listen({ onUnhandledRequest: 'error' });
});

afterEach(() => {
  server.resetHandlers();
  cleanup();
});

afterAll(() => {
  server.close();
});

// Mock environment variables
process.env.VITE_AUTH0_DOMAIN = TEST_CONSTANTS.AUTH0_TEST_DOMAIN;
process.env.VITE_AUTH0_CLIENT_ID = TEST_CONSTANTS.AUTH0_TEST_CLIENT_ID;
process.env.VITE_AUTH0_AUDIENCE = TEST_CONSTANTS.AUTH0_TEST_AUDIENCE;
process.env.VITE_AUTH0_REDIRECT_URI = TEST_CONSTANTS.AUTH0_TEST_REDIRECT_URI;
process.env.VITE_API_BASE_URL = TEST_CONSTANTS.TEST_API_BASE_URL;

// Mock Auth0
const mockAuth0 = {
  isAuthenticated: false,
  user: null,
  isLoading: false,
  loginWithRedirect: vi.fn(),
  logout: vi.fn(),
  getAccessTokenSilently: vi.fn().mockResolvedValue(TEST_CONSTANTS.MOCK_ACCESS_TOKEN),
  getIdTokenClaims: vi.fn().mockResolvedValue({
    'https://api.companies-register.govt.nz/roles': TEST_CONSTANTS.TEST_USER_ROLES
  }),
};

vi.mock('@auth0/auth0-react', () => ({
  useAuth0: () => mockAuth0,
  Auth0Provider: ({ children }: { children: React.ReactNode }) => children,
}));

// Mock window.location
Object.defineProperty(window, 'location', {
  value: {
    origin: 'http://localhost:3000',
    pathname: '/',
    search: '',
    hash: '',
    href: 'http://localhost:3000/',
  },
  writable: true,
});

// Mock console methods to reduce noise in tests
global.console = {
  ...console,
  log: vi.fn(),
  debug: vi.fn(),
  info: vi.fn(),
  warn: vi.fn(),
  error: vi.fn(),
};

// Mock lucide-react icons
vi.mock('lucide-react', () => {
  const MockIcon = ({ className, ...props }: { className?: string; [key: string]: unknown }) => {
    return React.createElement('svg', { className, ...props, 'data-testid': 'mock-icon' });
  };
  
  return {
    Building2: MockIcon,
    Search: MockIcon,
    Shield: MockIcon,
    Users: MockIcon,
    FileText: MockIcon,
    TrendingUp: MockIcon,
    Plus: MockIcon,
    CheckCircle: MockIcon,
    XCircle: MockIcon,
    AlertCircle: MockIcon,
    Loader2: MockIcon,
    Upload: MockIcon,
    X: MockIcon,
    UserPlus: MockIcon,
    Check: MockIcon,
    Calendar: MockIcon,
    MapPin: MockIcon,
    LogIn: MockIcon,
    LogOut: MockIcon,
    User: MockIcon,
  };
});

// Global test utilities
(global as typeof global & { mockAuth0: typeof mockAuth0 }).mockAuth0 = mockAuth0;