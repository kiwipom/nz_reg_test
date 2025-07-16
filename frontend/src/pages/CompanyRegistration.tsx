import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Building2, Check, AlertCircle, Loader2, Upload, X, FileText } from 'lucide-react';
import { useAuth } from '../auth/useAuth';
import { registrationService } from '../services/registrationService';

interface CompanyRegistrationData {
  companyName: string;
  companyType: 'LTD' | 'OVERSEAS' | 'UNLIMITED';
  incorporationDate: string;
  nzbn?: string;
  shareCapital: number;
  numberOfShares: number;
  shareValue: number;
  constitutionFile?: File;
}

interface NameAvailabilityStatus {
  isChecking: boolean;
  isAvailable: boolean | null;
  hasChecked: boolean;
}

export const CompanyRegistration: React.FC = () => {
  const navigate = useNavigate();
  const { getAccessToken } = useAuth();
  
  const [formData, setFormData] = useState<CompanyRegistrationData>({
    companyName: '',
    companyType: 'LTD',
    incorporationDate: '',
    nzbn: '',
    shareCapital: 100,
    numberOfShares: 100,
    shareValue: 1,
    constitutionFile: undefined,
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
      const data = await registrationService.checkNameAvailability(name);
      
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

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      // Validate file type
      const allowedTypes = ['application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'];
      if (!allowedTypes.includes(file.type)) {
        setErrors(prev => ({ ...prev, constitutionFile: 'Only PDF, DOC, and DOCX files are allowed' }));
        return;
      }

      // Validate file size (5MB limit)
      if (file.size > 5 * 1024 * 1024) {
        setErrors(prev => ({ ...prev, constitutionFile: 'File size must be less than 5MB' }));
        return;
      }

      setFormData(prev => ({ ...prev, constitutionFile: file }));
      setErrors(prev => ({ ...prev, constitutionFile: '' }));
    }
  };

  const removeFile = () => {
    setFormData(prev => ({ ...prev, constitutionFile: undefined }));
    setErrors(prev => ({ ...prev, constitutionFile: '' }));
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
      // Get authentication token
      const token = await getAccessToken();
      
      // Generate company number
      const companyNumber = registrationService.generateCompanyNumber();
      
      // Prepare registration data
      const registrationData = {
        companyNumber,
        companyName: formData.companyName,
        companyType: formData.companyType,
        incorporationDate: formData.incorporationDate,
        nzbn: formData.nzbn || undefined,
      };
      
      // Register company
      const result = await registrationService.registerCompany(registrationData, token);
      
      // Success - redirect to home page with success message
      navigate('/', { 
        state: { 
          message: `Company "${result.companyName}" registered successfully! Company Number: ${result.companyNumber}` 
        } 
      });
      
    } catch (error) {
      console.error('Error registering company:', error);
      const errorMessage = error instanceof Error ? error.message : 'Error registering company. Please try again.';
      alert(errorMessage);
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

            {/* Constitution File Upload */}
            <div>
              <label htmlFor="constitutionFile" className="block text-sm font-medium text-gray-700 mb-2">
                Constitution Document (Optional)
              </label>
              <p className="text-sm text-gray-600 mb-3">
                Upload the company constitution document (PDF, DOC, or DOCX format, max 5MB)
              </p>
              
              {!formData.constitutionFile ? (
                <div className="relative border-2 border-dashed border-gray-300 rounded-lg p-6 text-center hover:border-blue-400 transition-colors">
                  <div className="flex flex-col items-center">
                    <Upload className="h-12 w-12 text-gray-400 mb-3" />
                    <p className="text-sm text-gray-600 mb-2">
                      Click to upload or drag and drop
                    </p>
                    <p className="text-xs text-gray-500">
                      PDF, DOC, DOCX (max 5MB)
                    </p>
                  </div>
                  <input
                    type="file"
                    id="constitutionFile"
                    name="constitutionFile"
                    accept=".pdf,.doc,.docx"
                    onChange={handleFileUpload}
                    className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
                  />
                </div>
              ) : (
                <div className="bg-green-50 border border-green-200 rounded-lg p-4 flex items-center justify-between">
                  <div className="flex items-center">
                    <FileText className="h-5 w-5 text-green-600 mr-3" />
                    <div>
                      <p className="text-sm font-medium text-green-800">
                        {formData.constitutionFile.name}
                      </p>
                      <p className="text-xs text-green-600">
                        {(formData.constitutionFile.size / 1024 / 1024).toFixed(2)} MB
                      </p>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={removeFile}
                    className="text-red-600 hover:text-red-700 p-1 rounded-full hover:bg-red-50 transition-colors"
                  >
                    <X className="h-4 w-4" />
                  </button>
                </div>
              )}
              
              {errors.constitutionFile && (
                <p className="mt-2 text-sm text-red-600">{errors.constitutionFile}</p>
              )}
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