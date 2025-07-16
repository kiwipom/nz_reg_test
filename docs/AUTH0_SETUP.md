# Auth0 Setup Guide

This guide walks through setting up Auth0 for JWT authentication in the NZ Companies Register System.

## Why Auth0?

Auth0 was chosen for this implementation because:
- **Enterprise-grade security** suitable for government systems
- **Compliance certifications** (GDPR, HIPAA, SOC 2 Type II)
- **Robust JWT capabilities** with extensive customization
- **Free tier** supports up to 7,500 active users
- **Comprehensive documentation** and community support

## Auth0 Account Setup

### 1. Create Auth0 Account

1. Go to [Auth0 Dashboard](https://auth0.com/signup)
2. Sign up for a free account
3. Choose a unique tenant domain (e.g., `nz-companies-register-dev`)
4. Select your region (recommend `au` for Australia/New Zealand)

### 2. Create Application

1. Navigate to **Applications** in the Auth0 Dashboard
2. Click **Create Application**
3. Choose application name: `NZ Companies Register`
4. Select **Single Page Web Applications** (for React frontend)
5. Click **Create**

### 3. Configure Application Settings

In the application settings:

#### Basic Information
- **Name**: NZ Companies Register
- **Domain**: `your-tenant.au.auth0.com`
- **Client ID**: (copy this for frontend config)
- **Client Secret**: (not needed for SPA)

#### Application URIs
- **Allowed Callback URLs**: 
  ```
  http://localhost:3000/callback,
  https://your-production-domain.com/callback
  ```
- **Allowed Logout URLs**:
  ```
  http://localhost:3000,
  https://your-production-domain.com
  ```
- **Allowed Web Origins**:
  ```
  http://localhost:3000,
  https://your-production-domain.com
  ```

#### Advanced Settings
- **Grant Types**: Check `Authorization Code`, `Refresh Token`, `Implicit`
- **JsonWebToken Signature Algorithm**: `RS256`

### 4. Create API

1. Navigate to **APIs** in the Auth0 Dashboard
2. Click **Create API**
3. Configure:
   - **Name**: NZ Companies Register API
   - **Identifier**: `https://api.companies-register.govt.nz`
   - **Signing Algorithm**: `RS256`

### 5. Configure Scopes

In the API settings, add these scopes:
- `read:companies` - Read company information
- `write:companies` - Create/update companies
- `read:directors` - Read director information
- `write:directors` - Create/update directors
- `read:shareholders` - Read shareholder information
- `write:shareholders` - Create/update shareholders
- `admin:all` - Full administrative access

### 6. Set up Roles and Permissions

#### Create Roles

1. Go to **User Management** > **Roles**
2. Create these roles:

**Public Role**
- Description: Public users accessing company information
- Permissions: `read:companies`

**Registrar Role**
- Description: Companies Office registrars
- Permissions: `read:companies`, `write:companies`, `read:directors`, `write:directors`, `read:shareholders`, `write:shareholders`

**Internal Ops Role**
- Description: Internal operations staff
- Permissions: `read:companies`, `read:directors`, `read:shareholders`

**Admin Role**
- Description: System administrators
- Permissions: `admin:all`

#### Assign Permissions to Roles

1. Go to each role
2. Click **Permissions** tab
3. Click **Add Permissions**
4. Select the API and assign appropriate scopes

### 7. Configure Rules (Optional)

Create a Rule to add roles to JWT tokens:

1. Go to **Auth Pipeline** > **Rules**
2. Create a new rule: "Add Roles to JWT"
3. Use this code:

```javascript
function addRolesToTokens(user, context, callback) {
  const namespace = 'https://api.companies-register.govt.nz/';
  const assignedRoles = (context.authorization || {}).roles;
  
  let idTokenClaims = context.idToken || {};
  let accessTokenClaims = context.accessToken || {};
  
  idTokenClaims[`${namespace}roles`] = assignedRoles;
  accessTokenClaims[`${namespace}roles`] = assignedRoles;
  
  context.idToken = idTokenClaims;
  context.accessToken = accessTokenClaims;
  
  callback(null, user, context);
}
```

## Environment Variables

Create these environment variables for your applications:

### Backend (.env)
```env
AUTH0_DOMAIN=your-tenant.au.auth0.com
AUTH0_AUDIENCE=https://api.companies-register.govt.nz
AUTH0_ISSUER=https://your-tenant.au.auth0.com/
```

### Frontend (.env)
```env
VITE_AUTH0_DOMAIN=your-tenant.au.auth0.com
VITE_AUTH0_CLIENT_ID=your-client-id
VITE_AUTH0_AUDIENCE=https://api.companies-register.govt.nz
VITE_AUTH0_REDIRECT_URI=http://localhost:3000/callback
```

## Testing Users

Create test users for each role:

1. Go to **User Management** > **Users**
2. Create users:
   - `admin@nzco.test.nz`, `admin-p@ssw0rd` (Admin role)
   - `registrar@nzco.test.nz`, `registrar-p@ssw0rd` (Registrar role)
   - `internal@nzco.test.nz`, `internal-p@ssw0rd` (Internal Ops role)
   - `public@test.com`, `public-p@ssw0rd` (Public role)

## Security Considerations

1. **Never commit secrets** to version control
2. **Use different tenants** for dev/staging/production
3. **Enable MFA** for admin users
4. **Regularly rotate secrets** in production
5. **Monitor Auth0 logs** for suspicious activity
6. **Use HTTPS only** in production

## Troubleshooting

### Common Issues

1. **CORS errors**: Check Allowed Web Origins in application settings
2. **Token validation fails**: Verify audience and issuer configuration
3. **Roles not in token**: Check Rules configuration
4. **Callback URL errors**: Verify Allowed Callback URLs

### Debug Steps

1. Check Auth0 logs in Dashboard > Monitoring > Logs
2. Use Auth0 debugger extension for Chrome
3. Verify JWT tokens at [jwt.io](https://jwt.io)
4. Check browser network tab for auth requests

## Next Steps

After Auth0 setup:
1. Configure Spring Boot OAuth2 Resource Server
2. Implement JWT authentication in backend
3. Set up Auth0 React SDK in frontend
4. Test authentication flow end-to-end

## Resources

- [Auth0 Documentation](https://auth0.com/docs)
- [Auth0 React SDK](https://auth0.com/docs/libraries/auth0-react)
- [Auth0 Spring Boot Integration](https://auth0.com/docs/quickstart/backend/java-spring-boot)
- [JWT Token Format](https://auth0.com/docs/secure/tokens/json-web-tokens)