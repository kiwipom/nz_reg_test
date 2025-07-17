import React, { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { UserPlus, Check, Loader2, Calendar, MapPin } from 'lucide-react';
import { useAuth } from '../auth/useAuth';

interface DirectorFormData {
  firstName: string;
  lastName: string;
  middleName?: string;
  dateOfBirth: string;
  occupation: string;
  nationality: string;
  residencyStatus: 'NZ_CITIZEN' | 'NZ_RESIDENT' | 'AU_CITIZEN' | 'AU_RESIDENT' | 'OTHER';
  
  // Address information
  residentialAddress: {
    streetAddress: string;
    suburb: string;
    city: string;
    postcode: string;
    country: string;
  };
  
  // Alternative address for service (optional)
  serviceAddress?: {
    streetAddress: string;
    suburb: string;
    city: string;
    postcode: string;
    country: string;
  };
  useResidentialForService: boolean;
  
  // Consent and declarations
  hasConsentedToAppointment: boolean;
  hasReadDirectorDuties: boolean;
  isNotDisqualified: boolean;
  
  // Supporting documents
  consentDocument?: File;
  identificationDocument?: File;
  
  // Appointment details
  appointmentDate: string;
  directorRole?: string;
  hasSigningAuthority: boolean;
}

interface ValidationErrors {
  [key: string]: string;
}

export const DirectorAppointment: React.FC = () => {
  const navigate = useNavigate();
  const { companyId } = useParams();
  const { getAccessToken } = useAuth();
  
  const [formData, setFormData] = useState<DirectorFormData>({
    firstName: '',
    lastName: '',
    middleName: '',
    dateOfBirth: '',
    occupation: '',
    nationality: 'New Zealand',
    residencyStatus: 'NZ_CITIZEN',
    residentialAddress: {
      streetAddress: '',
      suburb: '',
      city: '',
      postcode: '',
      country: 'New Zealand',
    },
    useResidentialForService: true,
    hasConsentedToAppointment: false,
    hasReadDirectorDuties: false,
    isNotDisqualified: false,
    appointmentDate: new Date().toISOString().split('T')[0],
    hasSigningAuthority: false,
  });

  const [errors, setErrors] = useState<ValidationErrors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  const validateResidency = (status: string): boolean => {
    return ['NZ_CITIZEN', 'NZ_RESIDENT', 'AU_CITIZEN', 'AU_RESIDENT'].includes(status);
  };

  const validateForm = (): boolean => {
    const newErrors: ValidationErrors = {};

    // Required personal information
    if (!formData.firstName.trim()) {
      newErrors.firstName = 'First name is required';
    }
    if (!formData.lastName.trim()) {
      newErrors.lastName = 'Last name is required';
    }
    if (!formData.dateOfBirth) {
      newErrors.dateOfBirth = 'Date of birth is required';
    } else {
      const birthDate = new Date(formData.dateOfBirth);
      const today = new Date();
      const age = today.getFullYear() - birthDate.getFullYear();
      if (age < 18) {
        newErrors.dateOfBirth = 'Director must be at least 18 years old';
      }
    }
    if (!formData.occupation.trim()) {
      newErrors.occupation = 'Occupation is required';
    }

    // Residency validation
    if (!validateResidency(formData.residencyStatus)) {
      newErrors.residencyStatus = 'At least one director must be a New Zealand or Australian resident';
    }

    // Address validation
    if (!formData.residentialAddress.streetAddress.trim()) {
      newErrors['residentialAddress.streetAddress'] = 'Street address is required';
    }
    if (!formData.residentialAddress.city.trim()) {
      newErrors['residentialAddress.city'] = 'City is required';
    }
    if (!formData.residentialAddress.postcode.trim()) {
      newErrors['residentialAddress.postcode'] = 'Postcode is required';
    }

    // Service address validation (if different from residential)
    if (!formData.useResidentialForService) {
      if (!formData.serviceAddress?.streetAddress?.trim()) {
        newErrors['serviceAddress.streetAddress'] = 'Service address street is required';
      }
      if (!formData.serviceAddress?.city?.trim()) {
        newErrors['serviceAddress.city'] = 'Service address city is required';
      }
      if (!formData.serviceAddress?.postcode?.trim()) {
        newErrors['serviceAddress.postcode'] = 'Service address postcode is required';
      }
    }

    // Consent and declaration validation
    if (!formData.hasConsentedToAppointment) {
      newErrors.hasConsentedToAppointment = 'Director consent is required';
    }
    if (!formData.hasReadDirectorDuties) {
      newErrors.hasReadDirectorDuties = 'Acknowledgment of director duties is required';
    }
    if (!formData.isNotDisqualified) {
      newErrors.isNotDisqualified = 'Declaration of non-disqualification is required';
    }

    // Appointment date validation
    if (!formData.appointmentDate) {
      newErrors.appointmentDate = 'Appointment date is required';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value, type } = e.target;
    const checked = type === 'checkbox' ? (e.target as HTMLInputElement).checked : undefined;

    if (name.includes('.')) {
      const [parent, child] = name.split('.');
      setFormData(prev => ({
        ...prev,
        [parent]: {
          ...prev[parent as keyof DirectorFormData] as Record<string, unknown>,
          [child]: value,
        },
      }));
    } else {
      setFormData(prev => ({
        ...prev,
        [name]: type === 'checkbox' ? checked : value,
      }));
    }

    // Clear error when user starts typing
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  // File upload handler for future use
  // const handleFileUpload = (field: 'consentDocument' | 'identificationDocument') => 
  //   (e: React.ChangeEvent<HTMLInputElement>) => {
  //     const file = e.target.files?.[0];
  //     if (file) {
  //       // Validate file type
  //       const allowedTypes = ['application/pdf', 'image/jpeg', 'image/png'];
  //       if (!allowedTypes.includes(file.type)) {
  //         setErrors(prev => ({ 
  //           ...prev, 
  //           [field]: 'Only PDF, JPEG, and PNG files are allowed' 
  //         }));
  //         return;
  //       }

  //       // Validate file size (5MB limit)
  //       if (file.size > 5 * 1024 * 1024) {
  //         setErrors(prev => ({ 
  //           ...prev, 
  //           [field]: 'File size must be less than 5MB' 
  //         }));
  //         return;
  //       }

  //       setFormData(prev => ({ ...prev, [field]: file }));
  //       setErrors(prev => ({ ...prev, [field]: '' }));
  //     }
  //   };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    const isValid = validateForm();
    if (!isValid) {
      return;
    }

    setIsSubmitting(true);
    
    try {
      await getAccessToken();
      
      // Create FormData for file uploads
      const submitData = new FormData();
      submitData.append('directorData', JSON.stringify({
        ...formData,
        companyId,
        consentDocument: undefined,
        identificationDocument: undefined,
      }));

      if (formData.consentDocument) {
        submitData.append('consentDocument', formData.consentDocument);
      }
      if (formData.identificationDocument) {
        submitData.append('identificationDocument', formData.identificationDocument);
      }

      // TODO: Replace with actual API call
      console.log('Submitting director appointment:', formData);
      
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 2000));
      
      navigate(`/companies/${companyId}`, { 
        state: { 
          message: `Director ${formData.firstName} ${formData.lastName} appointed successfully!` 
        } 
      });
      
    } catch (error) {
      console.error('Error appointing director:', error);
      const errorMessage = error instanceof Error ? error.message : 'Error appointing director. Please try again.';
      alert(errorMessage);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-green-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-4xl mx-auto">
        <div className="text-center mb-8">
          <div className="flex items-center justify-center mb-4">
            <div className="flex items-center justify-center w-16 h-16 bg-gradient-to-br from-blue-600 to-green-600 rounded-xl shadow-lg">
              <UserPlus className="h-8 w-8 text-white" />
            </div>
          </div>
          <h1 className="text-3xl font-bold text-gray-900 mb-2">
            Appoint New Director
          </h1>
          <p className="text-lg text-gray-600">
            Add a new director to the company register
          </p>
        </div>

        <div className="bg-white rounded-2xl shadow-xl p-8 border border-gray-100">
          <form onSubmit={handleSubmit} noValidate className="space-y-8">
            
            {/* Personal Information Section */}
            <div className="border-b border-gray-200 pb-8">
              <h2 className="text-xl font-semibold text-gray-900 mb-6 flex items-center">
                <UserPlus className="h-5 w-5 mr-2" />
                Personal Information
              </h2>
              
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div>
                  <label htmlFor="firstName" className="block text-sm font-medium text-gray-700 mb-2">
                    First Name *
                  </label>
                  <input
                    type="text"
                    id="firstName"
                    name="firstName"
                    value={formData.firstName}
                    onChange={handleInputChange}
                    className={`w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors ${
                      errors.firstName ? 'border-red-500' : 'border-gray-300'
                    }`}
                    placeholder="Enter first name"
                    required
                  />
                  {errors.firstName && (
                    <p className="mt-2 text-sm text-red-600">{errors.firstName}</p>
                  )}
                </div>

                <div>
                  <label htmlFor="middleName" className="block text-sm font-medium text-gray-700 mb-2">
                    Middle Name(s)
                  </label>
                  <input
                    type="text"
                    id="middleName"
                    name="middleName"
                    value={formData.middleName}
                    onChange={handleInputChange}
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors"
                    placeholder="Enter middle name(s)"
                  />
                </div>

                <div>
                  <label htmlFor="lastName" className="block text-sm font-medium text-gray-700 mb-2">
                    Last Name *
                  </label>
                  <input
                    type="text"
                    id="lastName"
                    name="lastName"
                    value={formData.lastName}
                    onChange={handleInputChange}
                    className={`w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors ${
                      errors.lastName ? 'border-red-500' : 'border-gray-300'
                    }`}
                    placeholder="Enter last name"
                    required
                  />
                  {errors.lastName && (
                    <p className="mt-2 text-sm text-red-600">{errors.lastName}</p>
                  )}
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mt-6">
                <div>
                  <label htmlFor="dateOfBirth" className="block text-sm font-medium text-gray-700 mb-2">
                    Date of Birth *
                  </label>
                  <input
                    type="date"
                    id="dateOfBirth"
                    name="dateOfBirth"
                    value={formData.dateOfBirth}
                    onChange={handleInputChange}
                    className={`w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors ${
                      errors.dateOfBirth ? 'border-red-500' : 'border-gray-300'
                    }`}
                    required
                  />
                  {errors.dateOfBirth && (
                    <p className="mt-2 text-sm text-red-600">{errors.dateOfBirth}</p>
                  )}
                </div>

                <div>
                  <label htmlFor="occupation" className="block text-sm font-medium text-gray-700 mb-2">
                    Occupation *
                  </label>
                  <input
                    type="text"
                    id="occupation"
                    name="occupation"
                    value={formData.occupation}
                    onChange={handleInputChange}
                    className={`w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors ${
                      errors.occupation ? 'border-red-500' : 'border-gray-300'
                    }`}
                    placeholder="Enter occupation"
                    required
                  />
                  {errors.occupation && (
                    <p className="mt-2 text-sm text-red-600">{errors.occupation}</p>
                  )}
                </div>

                <div>
                  <label htmlFor="nationality" className="block text-sm font-medium text-gray-700 mb-2">
                    Nationality *
                  </label>
                  <input
                    type="text"
                    id="nationality"
                    name="nationality"
                    value={formData.nationality}
                    onChange={handleInputChange}
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors"
                    placeholder="Enter nationality"
                    required
                  />
                </div>
              </div>

              <div className="mt-6">
                <label htmlFor="residencyStatus" className="block text-sm font-medium text-gray-700 mb-2">
                  Residency Status *
                </label>
                <select
                  id="residencyStatus"
                  name="residencyStatus"
                  value={formData.residencyStatus}
                  onChange={handleInputChange}
                  className={`w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors ${
                    errors.residencyStatus ? 'border-red-500' : 'border-gray-300'
                  }`}
                  required
                >
                  <option value="NZ_CITIZEN">New Zealand Citizen</option>
                  <option value="NZ_RESIDENT">New Zealand Permanent Resident</option>
                  <option value="AU_CITIZEN">Australian Citizen</option>
                  <option value="AU_RESIDENT">Australian Permanent Resident</option>
                  <option value="OTHER">Other</option>
                </select>
                {errors.residencyStatus && (
                  <p className="mt-2 text-sm text-red-600">{errors.residencyStatus}</p>
                )}
                {!validateResidency(formData.residencyStatus) && (
                  <p className="mt-2 text-sm text-amber-600">
                    ⚠️ Note: At least one director must be a New Zealand or Australian resident
                  </p>
                )}
              </div>
            </div>

            {/* Address Information Section */}
            <div className="border-b border-gray-200 pb-8">
              <h2 className="text-xl font-semibold text-gray-900 mb-6 flex items-center">
                <MapPin className="h-5 w-5 mr-2" />
                Address Information
              </h2>
              
              <h3 className="text-lg font-medium text-gray-800 mb-4">Residential Address</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="md:col-span-2">
                  <label htmlFor="residentialAddress.streetAddress" className="block text-sm font-medium text-gray-700 mb-2">
                    Street Address *
                  </label>
                  <input
                    type="text"
                    id="residentialAddress.streetAddress"
                    name="residentialAddress.streetAddress"
                    value={formData.residentialAddress.streetAddress}
                    onChange={handleInputChange}
                    className={`w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors ${
                      errors['residentialAddress.streetAddress'] ? 'border-red-500' : 'border-gray-300'
                    }`}
                    placeholder="Enter street address"
                    required
                  />
                  {errors['residentialAddress.streetAddress'] && (
                    <p className="mt-2 text-sm text-red-600">{errors['residentialAddress.streetAddress']}</p>
                  )}
                </div>

                <div>
                  <label htmlFor="residentialAddress.suburb" className="block text-sm font-medium text-gray-700 mb-2">
                    Suburb
                  </label>
                  <input
                    type="text"
                    id="residentialAddress.suburb"
                    name="residentialAddress.suburb"
                    value={formData.residentialAddress.suburb}
                    onChange={handleInputChange}
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors"
                    placeholder="Enter suburb"
                  />
                </div>

                <div>
                  <label htmlFor="residentialAddress.city" className="block text-sm font-medium text-gray-700 mb-2">
                    City *
                  </label>
                  <input
                    type="text"
                    id="residentialAddress.city"
                    name="residentialAddress.city"
                    value={formData.residentialAddress.city}
                    onChange={handleInputChange}
                    className={`w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors ${
                      errors['residentialAddress.city'] ? 'border-red-500' : 'border-gray-300'
                    }`}
                    placeholder="Enter city"
                    required
                  />
                  {errors['residentialAddress.city'] && (
                    <p className="mt-2 text-sm text-red-600">{errors['residentialAddress.city']}</p>
                  )}
                </div>

                <div>
                  <label htmlFor="residentialAddress.postcode" className="block text-sm font-medium text-gray-700 mb-2">
                    Postcode *
                  </label>
                  <input
                    type="text"
                    id="residentialAddress.postcode"
                    name="residentialAddress.postcode"
                    value={formData.residentialAddress.postcode}
                    onChange={handleInputChange}
                    className={`w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors ${
                      errors['residentialAddress.postcode'] ? 'border-red-500' : 'border-gray-300'
                    }`}
                    placeholder="Enter postcode"
                    required
                  />
                  {errors['residentialAddress.postcode'] && (
                    <p className="mt-2 text-sm text-red-600">{errors['residentialAddress.postcode']}</p>
                  )}
                </div>

                <div>
                  <label htmlFor="residentialAddress.country" className="block text-sm font-medium text-gray-700 mb-2">
                    Country *
                  </label>
                  <input
                    type="text"
                    id="residentialAddress.country"
                    name="residentialAddress.country"
                    value={formData.residentialAddress.country}
                    onChange={handleInputChange}
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors"
                    placeholder="Enter country"
                    required
                  />
                </div>
              </div>

              <div className="mt-6">
                <div className="flex items-center">
                  <input
                    type="checkbox"
                    id="useResidentialForService"
                    name="useResidentialForService"
                    checked={formData.useResidentialForService}
                    onChange={handleInputChange}
                    className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                  />
                  <label htmlFor="useResidentialForService" className="ml-2 block text-sm text-gray-700">
                    Use residential address for service of documents
                  </label>
                </div>
              </div>

              {!formData.useResidentialForService && (
                <div className="mt-6">
                  <h3 className="text-lg font-medium text-gray-800 mb-4">Service Address</h3>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    {/* Service address fields - similar structure to residential */}
                    <div className="md:col-span-2">
                      <label htmlFor="serviceAddress.streetAddress" className="block text-sm font-medium text-gray-700 mb-2">
                        Street Address *
                      </label>
                      <input
                        type="text"
                        id="serviceAddress.streetAddress"
                        name="serviceAddress.streetAddress"
                        value={formData.serviceAddress?.streetAddress || ''}
                        onChange={(e) => {
                          setFormData(prev => ({
                            ...prev,
                            serviceAddress: {
                              ...prev.serviceAddress,
                              streetAddress: e.target.value,
                              suburb: prev.serviceAddress?.suburb || '',
                              city: prev.serviceAddress?.city || '',
                              postcode: prev.serviceAddress?.postcode || '',
                              country: prev.serviceAddress?.country || 'New Zealand',
                            }
                          }));
                        }}
                        className={`w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors ${
                          errors['serviceAddress.streetAddress'] ? 'border-red-500' : 'border-gray-300'
                        }`}
                        placeholder="Enter service address"
                        required
                      />
                      {errors['serviceAddress.streetAddress'] && (
                        <p className="mt-2 text-sm text-red-600">{errors['serviceAddress.streetAddress']}</p>
                      )}
                    </div>
                    {/* Additional service address fields would follow similar pattern */}
                  </div>
                </div>
              )}
            </div>

            {/* Consent and Declarations Section */}
            <div className="border-b border-gray-200 pb-8">
              <h2 className="text-xl font-semibold text-gray-900 mb-6 flex items-center">
                <Check className="h-5 w-5 mr-2" />
                Consent and Declarations
              </h2>
              
              <div className="space-y-6">
                <div className="flex items-start">
                  <input
                    type="checkbox"
                    id="hasConsentedToAppointment"
                    name="hasConsentedToAppointment"
                    checked={formData.hasConsentedToAppointment}
                    onChange={handleInputChange}
                    className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded mt-1"
                    required
                  />
                  <label htmlFor="hasConsentedToAppointment" className="ml-3 block text-sm text-gray-700">
                    I consent to being appointed as a director of this company *
                  </label>
                </div>
                {errors.hasConsentedToAppointment && (
                  <p className="text-sm text-red-600">{errors.hasConsentedToAppointment}</p>
                )}

                <div className="flex items-start">
                  <input
                    type="checkbox"
                    id="hasReadDirectorDuties"
                    name="hasReadDirectorDuties"
                    checked={formData.hasReadDirectorDuties}
                    onChange={handleInputChange}
                    className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded mt-1"
                    required
                  />
                  <label htmlFor="hasReadDirectorDuties" className="ml-3 block text-sm text-gray-700">
                    I have read and understand my duties and responsibilities as a director under the Companies Act 1993 *
                  </label>
                </div>
                {errors.hasReadDirectorDuties && (
                  <p className="text-sm text-red-600">{errors.hasReadDirectorDuties}</p>
                )}

                <div className="flex items-start">
                  <input
                    type="checkbox"
                    id="isNotDisqualified"
                    name="isNotDisqualified"
                    checked={formData.isNotDisqualified}
                    onChange={handleInputChange}
                    className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded mt-1"
                    required
                  />
                  <label htmlFor="isNotDisqualified" className="ml-3 block text-sm text-gray-700">
                    I declare that I am not disqualified from being a director under the Companies Act 1993 *
                  </label>
                </div>
                {errors.isNotDisqualified && (
                  <p className="text-sm text-red-600">{errors.isNotDisqualified}</p>
                )}
              </div>
            </div>

            {/* Appointment Details Section */}
            <div className="border-b border-gray-200 pb-8">
              <h2 className="text-xl font-semibold text-gray-900 mb-6 flex items-center">
                <Calendar className="h-5 w-5 mr-2" />
                Appointment Details
              </h2>
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <label htmlFor="appointmentDate" className="block text-sm font-medium text-gray-700 mb-2">
                    Appointment Date *
                  </label>
                  <input
                    type="date"
                    id="appointmentDate"
                    name="appointmentDate"
                    value={formData.appointmentDate}
                    onChange={handleInputChange}
                    className={`w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors ${
                      errors.appointmentDate ? 'border-red-500' : 'border-gray-300'
                    }`}
                    required
                  />
                  {errors.appointmentDate && (
                    <p className="mt-2 text-sm text-red-600">{errors.appointmentDate}</p>
                  )}
                </div>

                <div>
                  <label htmlFor="directorRole" className="block text-sm font-medium text-gray-700 mb-2">
                    Director Role (Optional)
                  </label>
                  <input
                    type="text"
                    id="directorRole"
                    name="directorRole"
                    value={formData.directorRole || ''}
                    onChange={handleInputChange}
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors"
                    placeholder="e.g., Managing Director, Chairman"
                  />
                </div>
              </div>

              <div className="mt-6">
                <div className="flex items-center">
                  <input
                    type="checkbox"
                    id="hasSigningAuthority"
                    name="hasSigningAuthority"
                    checked={formData.hasSigningAuthority}
                    onChange={handleInputChange}
                    className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                  />
                  <label htmlFor="hasSigningAuthority" className="ml-2 block text-sm text-gray-700">
                    This director has signing authority for the company
                  </label>
                </div>
              </div>
            </div>

            {/* Submit Button */}
            <div className="pt-6">
              <button
                type="submit"
                disabled={isSubmitting}
                className={`w-full py-3 px-6 rounded-lg font-medium text-white transition-all duration-200 ${
                  isSubmitting
                    ? 'bg-gray-400 cursor-not-allowed'
                    : 'bg-gradient-to-r from-blue-600 to-green-600 hover:from-blue-700 hover:to-green-700 shadow-lg hover:shadow-xl'
                }`}
              >
                {isSubmitting ? (
                  <div className="flex items-center justify-center">
                    <Loader2 className="h-5 w-5 animate-spin mr-2" />
                    Appointing Director...
                  </div>
                ) : (
                  'Appoint Director'
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};