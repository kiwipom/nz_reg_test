import { useAuth0 } from '@auth0/auth0-react';

export const useAuth = () => {
  const {
    user,
    isAuthenticated,
    isLoading,
    loginWithRedirect,
    logout,
    getAccessTokenSilently,
    getIdTokenClaims,
  } = useAuth0();

  const login = () => {
    loginWithRedirect({
      appState: { returnTo: window.location.pathname },
    });
  };

  const logoutUser = () => {
    logout({
      logoutParams: {
        returnTo: window.location.origin,
      },
    });
  };

  const getAccessToken = async () => {
    try {
      return await getAccessTokenSilently({
        authorizationParams: {
          audience: import.meta.env.VITE_AUTH0_AUDIENCE,
          scope: 'openid profile email read:companies write:companies read:directors write:directors read:shareholders write:shareholders',
        },
      });
    } catch (error) {
      console.error('Error getting access token:', error);
      throw error;
    }
  };

  const getUserRoles = async (): Promise<string[]> => {
    try {
      const claims = await getIdTokenClaims();
      const rolesClaim = claims?.['https://api.companies-register.govt.nz/roles'];
      return Array.isArray(rolesClaim) ? rolesClaim : [];
    } catch (error) {
      console.error('Error getting user roles:', error);
      return [];
    }
  };

  const hasRole = async (role: string): Promise<boolean> => {
    const roles = await getUserRoles();
    return roles.includes(role);
  };

  const hasAnyRole = async (roles: string[]): Promise<boolean> => {
    const userRoles = await getUserRoles();
    return roles.some(role => userRoles.includes(role));
  };

  return {
    user,
    isAuthenticated,
    isLoading,
    login,
    logout: logoutUser,
    getAccessToken,
    getUserRoles,
    hasRole,
    hasAnyRole,
  };
};