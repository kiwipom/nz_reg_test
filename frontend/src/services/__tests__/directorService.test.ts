import { describe, it, expect, beforeEach } from 'vitest';
import { DirectorService } from '../directorService';

describe('DirectorService', () => {
  let directorService: DirectorService;

  beforeEach(() => {
    directorService = new DirectorService();
  });

  describe('getCountryCode', () => {
    it('should convert New Zealand to NZ', () => {
      expect(directorService.getCountryCode('New Zealand')).toBe('NZ');
      expect(directorService.getCountryCode('new zealand')).toBe('NZ');
      expect(directorService.getCountryCode('NEW ZEALAND')).toBe('NZ');
    });

    it('should convert Australia to AU', () => {
      expect(directorService.getCountryCode('Australia')).toBe('AU');
      expect(directorService.getCountryCode('australia')).toBe('AU');
      expect(directorService.getCountryCode('AUSTRALIA')).toBe('AU');
    });

    it('should convert United States variants to US', () => {
      expect(directorService.getCountryCode('United States')).toBe('US');
      expect(directorService.getCountryCode('United States of America')).toBe('US');
      expect(directorService.getCountryCode('united states')).toBe('US');
      expect(directorService.getCountryCode('united states of america')).toBe('US');
    });

    it('should convert United Kingdom to GB', () => {
      expect(directorService.getCountryCode('United Kingdom')).toBe('GB');
      expect(directorService.getCountryCode('united kingdom')).toBe('GB');
    });

    it('should convert Canada to CA', () => {
      expect(directorService.getCountryCode('Canada')).toBe('CA');
      expect(directorService.getCountryCode('canada')).toBe('CA');
    });

    it('should default to NZ for unrecognized countries', () => {
      expect(directorService.getCountryCode('Unknown Country')).toBe('NZ');
      expect(directorService.getCountryCode('France')).toBe('NZ');
      expect(directorService.getCountryCode('')).toBe('NZ');
      expect(directorService.getCountryCode('Germany')).toBe('NZ');
    });

    it('should handle edge cases', () => {
      expect(directorService.getCountryCode('')).toBe('NZ');
      expect(directorService.getCountryCode('   ')).toBe('NZ');
      expect(directorService.getCountryCode('123')).toBe('NZ');
    });

    it('should ensure all returned codes are exactly 2 characters', () => {
      const testCountries = [
        'New Zealand',
        'Australia', 
        'United States',
        'United Kingdom',
        'Canada',
        'Unknown Country'
      ];

      testCountries.forEach(country => {
        const code = directorService.getCountryCode(country);
        expect(code).toHaveLength(2);
        expect(code).toMatch(/^[A-Z]{2}$/); // Should be exactly 2 uppercase letters
      });
    });

    it('should comply with database VARCHAR(2) constraint', () => {
      // This test specifically validates the database constraint issue we encountered
      const longCountryName = 'Very Long Country Name That Would Break Database';
      const code = directorService.getCountryCode(longCountryName);
      
      expect(code).toHaveLength(2);
      expect(code).toBe('NZ'); // Should default to NZ
      
      // Verify it would fit in PostgreSQL VARCHAR(2) column
      expect(code.length).toBeLessThanOrEqual(2);
    });
  });
});