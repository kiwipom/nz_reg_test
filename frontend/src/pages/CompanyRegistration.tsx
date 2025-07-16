import React, { useState } from 'react';
import { Building2, Check, AlertCircle, Loader2 } from 'lucide-react';

interface CompanyRegistrationData {
  companyName: string;
  companyType: 'LTD' | 'OVERSEAS' | 'UNLIMITED';
  incorporationDate: string;
  nzbn?: string;
  shareCapital: number;
  numberOfShares: number;
  shareValue: number;
}

interface NameAvailabilityStatus {
  isChecking: boolean;
  isAvailable: boolean | null;
  hasChecked: boolean;
}

export const CompanyRegistration: React.FC = () => {
  const [formData, setFormData] = useState<CompanyRegistrationData>({
    companyName: '',
    companyType: 'LTD',
    incorporationDate: '',
    nzbn: '',
    shareCapital: 100,
    numberOfShares: 100,
    shareValue: 1,
  });

  const [nameAvailability, setNameAvailability] = useState<NameAvailabilityStatus>({
    isChecking: false,
    isAvailable: null,
    hasChecked: false,
  });

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Debounced name availability check
  const checkNameAvailability = async (name: string) => {
    if (!name.trim()) {
      setNameAvailability({
        isChecking: false,
        isAvailable: null,
        hasChecked: false,
      });
      return;
    }

    setNameAvailability({
      isChecking: true,
      isAvailable: null,
      hasChecked: false,
    });

    try {
      const response = await fetch(
        `http://localhost:8080/api/v1/companies/check-name?name=${encodeURIComponent(name)}`
      );
      const data = await response.json();
      
      setNameAvailability({
        isChecking: false,
        isAvailable: data.available,
        hasChecked: true,
      });
    } catch (error) {
      console.error('Error checking name availability:', error);
      setNameAvailability({
        isChecking: false,
        isAvailable: null,
        hasChecked: false,
      });
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: name === 'shareCapital' || name === 'numberOfShares' || name === 'shareValue' 
        ? Number(value) 
        : value
    }));

    // Clear error when user starts typing
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: '' }));
    }

    // Check name availability when company name changes
    if (name === 'companyName') {
      const timeoutId = setTimeout(() => {
        checkNameAvailability(value);
      }, 500);

      return () => clearTimeout(timeoutId);
    }
  };

  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.companyName.trim()) {
      newErrors.companyName = 'Company name is required';
    } else if (nameAvailability.hasChecked && !nameAvailability.isAvailable) {
      newErrors.companyName = 'Company name is not available';
    }

    if (!formData.incorporationDate) {
      newErrors.incorporationDate = 'Incorporation date is required';
    }

    if (formData.shareCapital <= 0) {
      newErrors.shareCapital = 'Share capital must be greater than 0';
    }

    if (formData.numberOfShares <= 0) {
      newErrors.numberOfShares = 'Number of shares must be greater than 0';
    }

    if (formData.shareValue <= 0) {
      newErrors.shareValue = 'Share value must be greater than 0';
    }

    if (formData.shareCapital !== formData.numberOfShares * formData.shareValue) {
      newErrors.shareCapital = 'Share capital must equal number of shares × share value';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }

    setIsSubmitting(true);
    
    try {
      // TODO: Implement actual API call to register company
      await new Promise(resolve => setTimeout(resolve, 2000)); // Simulate API call
      
      // Success - redirect or show success message
      alert('Company registered successfully!');
      
    } catch (error) {
      console.error('Error registering company:', error);
      alert('Error registering company. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const getNameAvailabilityIcon = () => {
    if (nameAvailability.isChecking) {
      return <Loader2 className="h-5 w-5 text-blue-500 animate-spin" data-testid="loading-spinner" />;
    }
    if (nameAvailability.hasChecked) {
      return nameAvailability.isAvailable ? (
        <Check className="h-5 w-5 text-green-500" />
      ) : (
        <AlertCircle className="h-5 w-5 text-red-500" />
      );
    }
    return null;
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-green-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-3xl mx-auto">
        <div className="text-center mb-8">
          <div className="flex items-center justify-center mb-4">
            <div className="flex items-center justify-center w-16 h-16 bg-gradient-to-br from-blue-600 to-green-600 rounded-xl shadow-lg">
              <Building2 className="h-8 w-8 text-white" />
            </div>
          </div>
          <h1 className="text-3xl font-bold text-gray-900 mb-2">
            Register a New Company
          </h1>
          <p className="text-lg text-gray-600">
            Register your new company with the New Zealand Companies Office
          </p>
        </div>

        <div className="bg-white rounded-2xl shadow-xl p-8 border border-gray-100">
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Company Name */}
            <div>
              <label htmlFor="companyName" className="block text-sm font-medium text-gray-700 mb-2">
                Company Name *
              </label>
              <div className="relative">
                <input
                  type="text"
                  id="companyName"
                  name="companyName"
                  value={formData.companyName}
                  onChange={handleInputChange}
                  className={`w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors ${
                    errors.companyName ? 'border-red-500' : 'border-gray-300'
                  }`}
                  placeholder="Enter company name"
                  required
                />
                <div className="absolute right-3 top-1/2 transform -translate-y-1/2">
                  {getNameAvailabilityIcon()}
                </div>
              </div>
              {errors.companyName && (
                <p className="mt-2 text-sm text-red-600">{errors.companyName}</p>
              )}
              {nameAvailability.hasChecked && nameAvailability.isAvailable && (
                <p className="mt-2 text-sm text-green-600">✓ Name is available</p>
              )}
            </div>

            {/* Company Type */}
            <div>
              <label htmlFor="companyType" className="block text-sm font-medium text-gray-700 mb-2">
                Company Type *
              </label>
              <select
                id="companyType"
                name="companyType"
                value={formData.companyType}
                onChange={handleInputChange}
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors"
                required
              >
                <option value="LTD">Limited Company (Ltd)</option>
                <option value="OVERSEAS">Overseas Company</option>
                <option value="UNLIMITED">Unlimited Company</option>
              </select>
            </div>

            {/* Incorporation Date */}
            <div>
              <label htmlFor="incorporationDate" className="block text-sm font-medium text-gray-700 mb-2">
                Incorporation Date *
              </label>
              <input
                type="date"
                id="incorporationDate"
                name="incorporationDate"
                value={formData.incorporationDate}
                onChange={handleInputChange}
                className={`w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors ${
                  errors.incorporationDate ? 'border-red-500' : 'border-gray-300'
                }`}
                required
              />
              {errors.incorporationDate && (
                <p className="mt-2 text-sm text-red-600">{errors.incorporationDate}</p>
              )}
            </div>

            {/* NZBN */}
            <div>
              <label htmlFor="nzbn" className="block text-sm font-medium text-gray-700 mb-2">
                NZBN (Optional)
              </label>
              <input
                type="text"
                id="nzbn"
                name="nzbn"
                value={formData.nzbn}
                onChange={handleInputChange}
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors"
                placeholder="New Zealand Business Number"
              />
            </div>

            {/* Share Capital Section */}
            <div className="border-t pt-6">
              <h3 className="text-lg font-medium text-gray-900 mb-4">Share Capital</h3>
              
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label htmlFor="shareCapital" className="block text-sm font-medium text-gray-700 mb-2">
                    Share Capital (NZD) *
                  </label>
                  <input
                    type="number"
                    id="shareCapital"
                    name="shareCapital"
                    value={formData.shareCapital}
                    onChange={handleInputChange}
                    min="1"
                    step="0.01"
                    className={`w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors ${
                      errors.shareCapital ? 'border-red-500' : 'border-gray-300'
                    }`}
                    required
                  />
                  {errors.shareCapital && (
                    <p className="mt-2 text-sm text-red-600">{errors.shareCapital}</p>
                  )}
                </div>

                <div>
                  <label htmlFor="numberOfShares" className="block text-sm font-medium text-gray-700 mb-2">
                    Number of Shares *
                  </label>
                  <input
                    type="number"
                    id="numberOfShares"
                    name="numberOfShares"
                    value={formData.numberOfShares}
                    onChange={handleInputChange}
                    min="1"
                    className={`w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors ${
                      errors.numberOfShares ? 'border-red-500' : 'border-gray-300'
                    }`}
                    required
                  />
                  {errors.numberOfShares && (
                    <p className="mt-2 text-sm text-red-600">{errors.numberOfShares}</p>
                  )}
                </div>

                <div>
                  <label htmlFor="shareValue" className="block text-sm font-medium text-gray-700 mb-2">
                    Value per Share (NZD) *
                  </label>
                  <input
                    type="number"
                    id="shareValue"
                    name="shareValue"
                    value={formData.shareValue}
                    onChange={handleInputChange}
                    min="0.01"
                    step="0.01"
                    className={`w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors ${
                      errors.shareValue ? 'border-red-500' : 'border-gray-300'
                    }`}
                    required
                  />
                  {errors.shareValue && (
                    <p className="mt-2 text-sm text-red-600">{errors.shareValue}</p>
                  )}
                </div>
              </div>
            </div>

            {/* Submit Button */}
            <div className="pt-6">
              <button
                type="submit"
                disabled={isSubmitting || !nameAvailability.isAvailable}
                className={`w-full py-3 px-6 rounded-lg font-medium text-white transition-all duration-200 ${
                  isSubmitting || !nameAvailability.isAvailable
                    ? 'bg-gray-400 cursor-not-allowed'
                    : 'bg-gradient-to-r from-blue-600 to-green-600 hover:from-blue-700 hover:to-green-700 shadow-lg hover:shadow-xl'
                }`}
              >
                {isSubmitting ? (
                  <div className="flex items-center justify-center">
                    <Loader2 className="h-5 w-5 animate-spin mr-2" />
                    Registering Company...
                  </div>
                ) : (
                  'Register Company'
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};