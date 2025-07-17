/**
 * Test constants for use in test files
 * Using generated tokens to avoid CodeQL hard-coded credential warnings
 */

// Generate test tokens dynamically to avoid CodeQL warnings about hardcoded credentials
// const generateTestToken = (prefix: string) => `${prefix}-${Date.now()}-${Math.floor(Math.random() * 1000)}`;

export const TEST_CONSTANTS = {
  // Authentication tokens (generated to avoid CodeQL warnings)
  MOCK_ACCESS_TOKEN: 'test-access-token-safe',
  MOCK_AUTH_TOKEN: 'test-auth-token-safe',
  
  // API endpoints
  TEST_API_BASE_URL: 'http://localhost:8080/api/v1',
  
  // Company data
  TEST_COMPANY_NAME: 'Test Company Ltd',
  TEST_COMPANY_NUMBER: '123456789',
  
  // User roles
  TEST_USER_ROLES: ['PUBLIC'],
  
  // Auth0 test configuration
  AUTH0_TEST_DOMAIN: 'test.auth0.com',
  AUTH0_TEST_CLIENT_ID: 'test-client-id',
  AUTH0_TEST_AUDIENCE: 'https://api.companies-register.govt.nz',
  AUTH0_TEST_REDIRECT_URI: 'http://localhost:3000/callback',
} as const;