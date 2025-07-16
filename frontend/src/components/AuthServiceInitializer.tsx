import React, { useEffect } from 'react';
import { useAuth } from '../auth/useAuth';
import { companyService } from '../services/companyService';

interface Props {
  children: React.ReactNode;
}

export const AuthServiceInitializer: React.FC<Props> = ({ children }) => {
  const { getAccessToken, isAuthenticated } = useAuth();

  useEffect(() => {
    if (isAuthenticated) {
      companyService.setTokenFunction(getAccessToken);
    }
  }, [isAuthenticated, getAccessToken]);

  return <>{children}</>;
};