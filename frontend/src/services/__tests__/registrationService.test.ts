import { vi, beforeEach, describe, it, expect } from 'vitest';
import { RegistrationService } from '../registrationService';
import { TEST_CONSTANTS } from '../../test/constants';

// Mock fetch
const mockFetch = vi.fn();

describe('RegistrationService', () => {
  let service: RegistrationService;

  beforeEach(() => {
    service = new RegistrationService();
    mockFetch.mockClear();
    mockFetch.mockReset();
    // Set up the mock properly for each test
    vi.stubGlobal('fetch', mockFetch);
  });

  describe('checkNameAvailability', () => {
    it('returns availability status for valid name', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({ available: true }),
      });

      const result = await service.checkNameAvailability('Test Company Ltd');

      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/companies/check-name?name=Test%20Company%20Ltd'
      );
      expect(result).toEqual({ available: true });
    });

    it('handles special characters in company name', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({ available: false }),
      });

      const result = await service.checkNameAvailability('Test & Company (NZ) Ltd');

      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/companies/check-name?name=Test%20%26%20Company%20(NZ)%20Ltd'
      );
      expect(result).toEqual({ available: false });
    });

    it('throws error on HTTP error response', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 500,
      });

      await expect(service.checkNameAvailability('Test Company'))
        .rejects
        .toThrow('HTTP error! status: 500');
    });

    it('throws error on network failure', async () => {
      mockFetch.mockRejectedValueOnce(new Error('Network error'));

      await expect(service.checkNameAvailability('Test Company'))
        .rejects
        .toThrow('Network error');
    });
  });

  describe('checkNumberAvailability', () => {
    it('returns availability status for valid number', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({ available: true }),
      });

      const result = await service.checkNumberAvailability('1234567');

      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/companies/check-number?number=1234567'
      );
      expect(result).toEqual({ available: true });
    });

    it('handles special characters in company number', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({ available: false }),
      });

      const result = await service.checkNumberAvailability('NZBN-123456');

      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/companies/check-number?number=NZBN-123456'
      );
      expect(result).toEqual({ available: false });
    });

    it('throws error on HTTP error response', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 400,
      });

      await expect(service.checkNumberAvailability('INVALID'))
        .rejects
        .toThrow('HTTP error! status: 400');
    });
  });

  describe('registerCompany', () => {
    const mockCompanyData = {
      companyNumber: '1234567',
      companyName: 'Test Company Ltd',
      companyType: 'LTD' as const,
      incorporationDate: '2024-01-01',
      nzbn: '1234567890123',
    };

    const mockResponse = {
      id: 1,
      ...mockCompanyData,
      status: 'ACTIVE',
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      version: 1,
    };

    it('registers company successfully with authentication', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockResponse,
      });

      const result = await service.registerCompany(mockCompanyData, TEST_CONSTANTS.MOCK_AUTH_TOKEN);

      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/companies',
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${TEST_CONSTANTS.MOCK_AUTH_TOKEN}`,
          },
          body: JSON.stringify(mockCompanyData),
        }
      );
      expect(result).toEqual(mockResponse);
    });

    it('registers company successfully without authentication', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockResponse,
      });

      const result = await service.registerCompany(mockCompanyData);

      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/companies',
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(mockCompanyData),
        }
      );
      expect(result).toEqual(mockResponse);
    });

    it('registers company without optional NZBN', async () => {
      const dataWithoutNZBN = {
        ...mockCompanyData,
        nzbn: undefined,
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({ ...mockResponse, nzbn: undefined }),
      });

      const result = await service.registerCompany(dataWithoutNZBN, TEST_CONSTANTS.MOCK_AUTH_TOKEN);

      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/companies',
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${TEST_CONSTANTS.MOCK_AUTH_TOKEN}`,
          },
          body: JSON.stringify(dataWithoutNZBN),
        }
      );
      expect(result).toEqual({ ...mockResponse, nzbn: undefined });
    });

    it('throws error on HTTP error response', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 400,
        text: async () => 'Validation error',
      });

      await expect(service.registerCompany(mockCompanyData, TEST_CONSTANTS.MOCK_AUTH_TOKEN))
        .rejects
        .toThrow('Registration failed: 400 Validation error');
    });

    it('throws error on network failure', async () => {
      mockFetch.mockRejectedValueOnce(new Error('Network error'));

      await expect(service.registerCompany(mockCompanyData, TEST_CONSTANTS.MOCK_AUTH_TOKEN))
        .rejects
        .toThrow('Network error');
    });

    it('handles unauthorized access', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 401,
        text: async () => 'Unauthorized',
      });

      await expect(service.registerCompany(mockCompanyData, 'invalid-token'))
        .rejects
        .toThrow('Registration failed: 401 Unauthorized');
    });
  });

  describe('generateCompanyNumber', () => {
    it('generates a 9-digit company number', () => {
      const companyNumber = service.generateCompanyNumber();
      
      expect(companyNumber).toMatch(/^\d{9}$/);
    });

    it('generates unique company numbers', () => {
      const numbers = new Set();
      
      // Mock Date.now to return different values and Math.random to be deterministic
      let mockTime = 1640995200000;
      let mockRandom = 0;
      
      vi.spyOn(Date, 'now').mockImplementation(() => mockTime++);
      vi.spyOn(Math, 'random').mockImplementation(() => (mockRandom++ % 1000) / 1000);
      
      // Generate 100 numbers and check for uniqueness
      for (let i = 0; i < 100; i++) {
        numbers.add(service.generateCompanyNumber());
      }
      
      expect(numbers.size).toBe(100);
      
      // Restore original functions
      vi.restoreAllMocks();
    });

    it('generates numbers with timestamp component', () => {
      const originalNow = Date.now;
      const mockTimestamp = 1640995200000; // 2022-01-01T00:00:00Z
      
      Date.now = vi.fn(() => mockTimestamp);
      
      const companyNumber = service.generateCompanyNumber();
      
      // Should contain the last 6 digits of the timestamp
      expect(companyNumber).toMatch(/^200000\d{3}$/);
      
      Date.now = originalNow;
    });
  });

  describe('error handling', () => {
    it('handles malformed JSON response', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => {
          throw new Error('Invalid JSON');
        },
      });

      await expect(service.checkNameAvailability('Test Company'))
        .rejects
        .toThrow('Invalid JSON');
    });

    it('handles empty response', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => null,
      });

      const result = await service.checkNameAvailability('Test Company');
      expect(result).toBeNull();
    });
  });
});