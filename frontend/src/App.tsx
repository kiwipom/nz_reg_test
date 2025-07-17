import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import { Auth0ProviderWithHistory } from './auth/Auth0Provider';
import { AuthServiceInitializer } from './components/AuthServiceInitializer';
import { AuthButton } from './components/AuthButton';
import { HomePage } from './pages/HomePage';
import { CompanyRegistration } from './pages/CompanyRegistration';
import { CompanyDetails } from './pages/CompanyDetails';
import { DirectorAppointment } from './pages/DirectorAppointment';
import { DirectorResignation } from './pages/DirectorResignation';
import { Building2 } from 'lucide-react';

function App() {
  return (
    <Auth0ProviderWithHistory>
      <AuthServiceInitializer>
        <Router>
          <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-green-50">
            {/* Header */}
            <header className="bg-white shadow-lg border-b border-gray-200">
              <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                <div className="flex justify-between items-center py-4">
                  <div className="flex items-center space-x-3">
                    <Link to="/" className="flex items-center space-x-3">
                      <div className="flex items-center justify-center w-12 h-12 bg-gradient-to-br from-blue-600 to-green-600 rounded-xl">
                        <Building2 className="h-7 w-7 text-white" />
                      </div>
                      <div>
                        <h1 className="text-2xl font-bold bg-gradient-to-r from-blue-600 to-green-600 bg-clip-text text-transparent">
                          NZ Companies Register
                        </h1>
                        <p className="text-sm text-gray-500">New Zealand Government Service</p>
                      </div>
                    </Link>
                  </div>
                  <AuthButton />
                </div>
              </div>
            </header>

            {/* Main Content */}
            <main>
              <Routes>
                <Route path="/" element={<HomePage />} />
                <Route path="/register" element={<CompanyRegistration />} />
                <Route path="/companies/:id" element={<CompanyDetails />} />
                <Route path="/companies/:id/directors/appoint" element={<DirectorAppointment />} />
                <Route path="/companies/:id/directors/:directorId/resign" element={<DirectorResignation />} />
              </Routes>
            </main>

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
        </Router>
      </AuthServiceInitializer>
    </Auth0ProviderWithHistory>
  );
}

export default App;
