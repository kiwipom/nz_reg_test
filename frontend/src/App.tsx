import { Auth0ProviderWithHistory } from './auth/Auth0Provider';
import { AuthServiceInitializer } from './components/AuthServiceInitializer';
import { AuthButton } from './components/AuthButton';
import { CompanySearch } from './components/CompanySearch';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Building2, Search, Shield, Users, FileText, TrendingUp } from 'lucide-react';

function App() {
  return (
    <Auth0ProviderWithHistory>
      <AuthServiceInitializer>
        <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-green-50">
          {/* Header */}
          <header className="bg-white shadow-lg border-b border-gray-200">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
              <div className="flex justify-between items-center py-4">
                <div className="flex items-center space-x-3">
                  <div className="flex items-center justify-center w-12 h-12 bg-gradient-to-br from-blue-600 to-green-600 rounded-xl">
                    <Building2 className="h-7 w-7 text-white" />
                  </div>
                  <div>
                    <h1 className="text-2xl font-bold bg-gradient-to-r from-blue-600 to-green-600 bg-clip-text text-transparent">
                      NZ Companies Register
                    </h1>
                    <p className="text-sm text-gray-500">New Zealand Government Service</p>
                  </div>
                </div>
                <AuthButton />
              </div>
            </div>
          </header>

          {/* Hero Section */}
          <section className="relative py-20 px-4 sm:px-6 lg:px-8">
            <div className="max-w-7xl mx-auto">
              <div className="text-center">
                <h2 className="text-4xl font-bold text-gray-900 mb-6">
                  Find Company Information
                </h2>
                <p className="text-xl text-gray-600 max-w-3xl mx-auto mb-12">
                  Search the official New Zealand Companies Register for company details, 
                  directors, shareholders, and compliance information.
                </p>
                
                {/* Search Section */}
                <div className="max-w-4xl mx-auto mb-16">
                  <div className="bg-white rounded-2xl shadow-xl p-8 border border-gray-100 backdrop-blur-sm">
                    <div className="flex items-center justify-center mb-6">
                      <div className="flex items-center justify-center w-16 h-16 bg-gradient-to-br from-blue-100 to-green-100 rounded-full shadow-lg">
                        <Search className="h-8 w-8 text-blue-600" />
                      </div>
                    </div>
                    <CompanySearch />
                  </div>
                </div>
              </div>
            </div>
          </section>

          {/* Features Section */}
          <section className="py-16 px-4 sm:px-6 lg:px-8 bg-white">
            <div className="max-w-7xl mx-auto">
              <div className="text-center mb-12">
                <h3 className="text-3xl font-bold text-gray-900 mb-4">
                  Comprehensive Company Data
                </h3>
                <p className="text-lg text-gray-600">
                  Access detailed information about New Zealand companies and their compliance status
                </p>
              </div>
              
              <div className="grid md:grid-cols-3 gap-8">
                <div className="text-center p-8 rounded-xl bg-gradient-to-br from-blue-50 to-blue-100 border border-blue-200 shadow-sm hover:shadow-md transition-shadow duration-200">
                  <div className="flex items-center justify-center w-16 h-16 bg-blue-600 rounded-full mx-auto mb-4 shadow-md">
                    <Building2 className="h-8 w-8 text-white" />
                  </div>
                  <h4 className="text-xl font-semibold text-gray-900 mb-3">Company Details</h4>
                  <p className="text-gray-600">
                    View company registration details, incorporation dates, and current status
                  </p>
                </div>
                
                <div className="text-center p-8 rounded-xl bg-gradient-to-br from-green-50 to-green-100 border border-green-200 shadow-sm hover:shadow-md transition-shadow duration-200">
                  <div className="flex items-center justify-center w-16 h-16 bg-green-600 rounded-full mx-auto mb-4 shadow-md">
                    <Users className="h-8 w-8 text-white" />
                  </div>
                  <h4 className="text-xl font-semibold text-gray-900 mb-3">Directors & Shareholders</h4>
                  <p className="text-gray-600">
                    Access information about company directors and shareholders
                  </p>
                </div>
                
                <div className="text-center p-8 rounded-xl bg-gradient-to-br from-purple-50 to-purple-100 border border-purple-200 shadow-sm hover:shadow-md transition-shadow duration-200">
                  <div className="flex items-center justify-center w-16 h-16 bg-purple-600 rounded-full mx-auto mb-4 shadow-md">
                    <FileText className="h-8 w-8 text-white" />
                  </div>
                  <h4 className="text-xl font-semibold text-gray-900 mb-3">Compliance Records</h4>
                  <p className="text-gray-600">
                    Check annual returns, filing status, and compliance history
                  </p>
                </div>
              </div>
            </div>
          </section>

          {/* Protected Features Section */}
          <section className="py-16 px-4 sm:px-6 lg:px-8 bg-gray-50">
            <div className="max-w-7xl mx-auto">
              <ProtectedRoute requiredRoles={['ADMIN', 'REGISTRAR', 'INTERNAL_OPS']}>
                <div className="text-center mb-12">
                  <h3 className="text-3xl font-bold text-gray-900 mb-4">
                    Administrative Features
                  </h3>
                  <p className="text-lg text-gray-600">
                    Advanced tools for authorized users
                  </p>
                </div>
                
                <div className="grid md:grid-cols-2 gap-8">
                  <ProtectedRoute requiredRoles={['ADMIN', 'REGISTRAR']}>
                    <div className="bg-white rounded-xl shadow-lg p-8 border border-blue-200 hover:shadow-xl transition-shadow duration-200">
                      <div className="flex items-center mb-6">
                        <div className="flex items-center justify-center w-12 h-12 bg-gradient-to-br from-blue-600 to-blue-700 rounded-lg mr-4 shadow-md">
                          <Shield className="h-6 w-6 text-white" />
                        </div>
                        <h4 className="text-xl font-semibold text-gray-900">
                          Management Access
                        </h4>
                      </div>
                      <p className="text-gray-600 mb-4">
                        You have access to company management features including:
                      </p>
                      <ul className="space-y-3 text-gray-600">
                        <li className="flex items-center">
                          <TrendingUp className="h-4 w-4 text-green-500 mr-3" />
                          Company registration and updates
                        </li>
                        <li className="flex items-center">
                          <TrendingUp className="h-4 w-4 text-green-500 mr-3" />
                          Director and shareholder management
                        </li>
                        <li className="flex items-center">
                          <TrendingUp className="h-4 w-4 text-green-500 mr-3" />
                          Compliance monitoring
                        </li>
                      </ul>
                    </div>
                  </ProtectedRoute>
                  
                  <ProtectedRoute requiredRoles={['ADMIN']}>
                    <div className="bg-white rounded-xl shadow-lg p-8 border border-red-200 hover:shadow-xl transition-shadow duration-200">
                      <div className="flex items-center mb-6">
                        <div className="flex items-center justify-center w-12 h-12 bg-gradient-to-br from-red-600 to-red-700 rounded-lg mr-4 shadow-md">
                          <Shield className="h-6 w-6 text-white" />
                        </div>
                        <h4 className="text-xl font-semibold text-gray-900">
                          Administrator Access
                        </h4>
                      </div>
                      <p className="text-gray-600 mb-4">
                        Full administrative access to the system including:
                      </p>
                      <ul className="space-y-3 text-gray-600">
                        <li className="flex items-center">
                          <TrendingUp className="h-4 w-4 text-green-500 mr-3" />
                          System configuration and settings
                        </li>
                        <li className="flex items-center">
                          <TrendingUp className="h-4 w-4 text-green-500 mr-3" />
                          User management and permissions
                        </li>
                        <li className="flex items-center">
                          <TrendingUp className="h-4 w-4 text-green-500 mr-3" />
                          Advanced reporting and analytics
                        </li>
                      </ul>
                    </div>
                  </ProtectedRoute>
                </div>
              </ProtectedRoute>
            </div>
          </section>

          {/* Footer */}
          <footer className="bg-gray-900 text-white py-12">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
              <div className="text-center">
                <div className="flex items-center justify-center mb-4">
                  <div className="flex items-center justify-center w-10 h-10 bg-gradient-to-br from-blue-600 to-green-600 rounded-lg mr-3">
                    <Building2 className="h-5 w-5 text-white" />
                  </div>
                  <h3 className="text-lg font-semibold">NZ Companies Register</h3>
                </div>
                <p className="text-gray-400 mb-4">
                  Official New Zealand Government Service
                </p>
                <p className="text-sm text-gray-500">
                  © 2024 New Zealand Companies Office. All rights reserved.
                </p>
              </div>
            </div>
          </footer>
        </div>
      </AuthServiceInitializer>
    </Auth0ProviderWithHistory>
  );
}

export default App;
