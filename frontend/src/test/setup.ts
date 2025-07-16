import '@testing-library/jest-dom';
import { beforeAll, afterEach, afterAll, vi } from 'vitest';
import { cleanup } from '@testing-library/react';
import { server } from './mocks/server';

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
process.env.VITE_AUTH0_DOMAIN = 'test.auth0.com';
process.env.VITE_AUTH0_CLIENT_ID = 'test-client-id';
process.env.VITE_AUTH0_AUDIENCE = 'https://api.companies-register.govt.nz';
process.env.VITE_AUTH0_REDIRECT_URI = 'http://localhost:3000/callback';
process.env.VITE_API_BASE_URL = 'http://localhost:8080/api';

// Mock Auth0
const mockAuth0 = {
  isAuthenticated: false,
  user: null,
  isLoading: false,
  loginWithRedirect: vi.fn(),
  logout: vi.fn(),
  getAccessTokenSilently: vi.fn().mockResolvedValue('mock-token'),
  getIdTokenClaims: vi.fn().mockResolvedValue({
    'https://api.companies-register.govt.nz/roles': ['PUBLIC']
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

// Global test utilities
(global as typeof global & { mockAuth0: typeof mockAuth0 }).mockAuth0 = mockAuth0;