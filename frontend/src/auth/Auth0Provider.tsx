import React from 'react';
import { Auth0Provider } from '@auth0/auth0-react';

const domain = import.meta.env.VITE_AUTH0_DOMAIN;
const clientId = import.meta.env.VITE_AUTH0_CLIENT_ID;
const audience = import.meta.env.VITE_AUTH0_AUDIENCE;
const redirectUri = import.meta.env.VITE_AUTH0_REDIRECT_URI || window.location.origin;

interface Props {
  children: React.ReactNode;
}

export const Auth0ProviderWithHistory: React.FC<Props> = ({ children }) => {
  return (
    <Auth0Provider
      domain={domain}
      clientId={clientId}
      authorizationParams={{
        redirect_uri: redirectUri,
        audience: audience,
        scope: 'openid profile email read:companies write:companies read:directors write:directors read:shareholders write:shareholders',
      }}
      useRefreshTokens
      cacheLocation="localstorage"
    >
      {children}
    </Auth0Provider>
  );
};