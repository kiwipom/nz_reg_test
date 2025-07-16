import React from 'react';
import { Auth0ProviderWithHistory } from './auth/Auth0Provider';
import { AuthServiceInitializer } from './components/AuthServiceInitializer';
import { AuthButton } from './components/AuthButton';
import { CompanySearch } from './components/CompanySearch';
import { ProtectedRoute } from './components/ProtectedRoute';

function App() {
  return (
    <Auth0ProviderWithHistory>
      <AuthServiceInitializer>
        <div className="min-h-screen bg-gray-50">
          <header className="bg-white shadow">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
              <div className="flex justify-between items-center py-4">
                <div className="flex items-center">
                  <h1 className="text-2xl font-bold text-gray-900">
                    NZ Companies Register
                  </h1>
                </div>
                <AuthButton />
              </div>
            </div>
          </header>

          <main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
            <div className="px-4 py-6 sm:px-0">
              <div className="border-4 border-dashed border-gray-200 rounded-lg p-8">
                <div className="text-center mb-8">
                  <h2 className="text-3xl font-bold text-gray-900 mb-4">
                    Company Search
                  </h2>
                  <p className="text-gray-600">
                    Search for companies in the New Zealand Companies Register
                  </p>
                </div>
                
                <CompanySearch />
                
                <div className="mt-12">
                  <h3 className="text-xl font-semibold text-gray-900 mb-4">
                    Protected Features
                  </h3>
                  
                  <ProtectedRoute requiredRoles={['ADMIN', 'REGISTRAR']}>
                    <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-4">
                      <h4 className="font-medium text-blue-900 mb-2">
                        Admin/Registrar Features
                      </h4>
                      <p className="text-blue-700">
                        You have access to company management features.
                      </p>
                    </div>
                  </ProtectedRoute>
                  
                  <ProtectedRoute requiredRoles={['ADMIN']}>
                    <div className="bg-red-50 border border-red-200 rounded-lg p-4">
                      <h4 className="font-medium text-red-900 mb-2">
                        Admin Only Features
                      </h4>
                      <p className="text-red-700">
                        You have full administrative access to the system.
                      </p>
                    </div>
                  </ProtectedRoute>
                </div>
              </div>
            </div>
          </main>
        </div>
      </AuthServiceInitializer>
    </Auth0ProviderWithHistory>
  );
}

export default App;
