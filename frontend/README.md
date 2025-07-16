# Frontend - NZ Companies Register

React/TypeScript frontend application for the NZ Companies Register system, built with modern web technologies for accessibility and performance.

## 🏗️ Architecture

- **Framework**: React 18 with TypeScript
- **Build Tool**: Vite
- **Styling**: TailwindCSS + ShadCN/UI components
- **Forms**: React Hook Form + Zod validation
- **State Management**: Zustand
- **Routing**: React Router
- **Testing**: Jest + React Testing Library

## 🚀 Quick Start

### Prerequisites
- Node.js 18+
- npm or yarn

### Local Development

1. **Install dependencies**:
   ```bash
   npm install
   ```

2. **Start development server**:
   ```bash
   npm run dev
   ```

3. **Access the application**:
   - Frontend: http://localhost:5173
   - Hot reload enabled for development

### Build for Production

```bash
# Build optimized production bundle
npm run build

# Preview production build
npm run preview
```

## 📁 Project Structure

```
frontend/
├── src/
│   ├── components/          # Reusable UI components
│   │   ├── ui/             # ShadCN/UI base components
│   │   ├── forms/          # Form components
│   │   ├── layout/         # Layout components
│   │   └── common/         # Common components
│   ├── pages/              # Page components
│   │   ├── companies/      # Company-related pages
│   │   ├── directors/      # Director management
│   │   ├── search/         # Search functionality
│   │   └── admin/          # Admin pages
│   ├── hooks/              # Custom React hooks
│   ├── services/           # API services
│   ├── stores/             # Zustand stores
│   ├── types/              # TypeScript types
│   ├── utils/              # Utility functions
│   ├── styles/             # Global styles
│   └── App.tsx             # Main App component
├── public/                 # Static assets
├── index.html              # HTML template
├── package.json            # Dependencies and scripts
├── tailwind.config.js      # Tailwind configuration
├── tsconfig.json           # TypeScript configuration
└── vite.config.ts          # Vite configuration
```

## 🧪 Testing

### Run Tests
```bash
# Run all tests
npm test

# Run tests in watch mode
npm run test:watch

# Run tests with coverage
npm run test:coverage

# Run specific test file
npm test -- CompanyForm.test.tsx
```

### Test Coverage
- **Target**: 80%+ line coverage
- **Reports**: `coverage/lcov-report/index.html`

### Test Types
- **Unit Tests**: Component logic and utilities
- **Integration Tests**: Component interactions
- **E2E Tests**: User workflows (Playwright)

## 🔧 Development

### Code Quality
```bash
# Check TypeScript types
npm run typecheck

# Lint code
npm run lint

# Fix linting issues
npm run lint:fix

# Format code
npm run format

# Check formatting
npm run format:check
```

### Build & Package
```bash
# Build for production
npm run build

# Build Docker image
docker build -t nz-companies-register-frontend .

# Analyze bundle size
npm run build:analyze
```

## 🎨 Styling

### TailwindCSS
- **Configuration**: `tailwind.config.js`
- **Base styles**: `src/styles/index.css`
- **Components**: Utility-first approach

### ShadCN/UI Components
```bash
# Add new component
npx shadcn-ui@latest add button

# List available components
npx shadcn-ui@latest list
```

### Custom Styling
```css
/* src/styles/index.css */
@tailwind base;
@tailwind components;
@tailwind utilities;

@layer base {
  :root {
    --background: 0 0% 100%;
    --foreground: 222.2 84% 4.9%;
    /* ... */
  }
}
```

## 🔐 Security

### Authentication
- **Type**: JWT tokens from backend
- **Storage**: HTTP-only cookies (production)
- **Refresh**: Automatic token refresh

### Authorization
- **Route Protection**: Private routes with auth checks
- **Role-based Access**: Admin, Registrar, Public roles
- **Component-level**: Conditional rendering based on permissions

### Development Security
```typescript
// Mock authentication for development
const mockAuth = {
  isAuthenticated: true,
  user: { role: 'admin' },
  token: 'mock-token'
};
```

## 📊 State Management

### Zustand Stores
```typescript
// src/stores/authStore.ts
export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: false,
  login: (credentials) => {
    // Login logic
  },
  logout: () => {
    // Logout logic
  }
}));
```

### Store Structure
- **authStore**: Authentication state
- **companyStore**: Company data and operations
- **searchStore**: Search state and results
- **uiStore**: UI state (modals, notifications)

## 🌐 API Integration

### Service Layer
```typescript
// src/services/companyService.ts
export const companyService = {
  async searchCompanies(query: string): Promise<Company[]> {
    const response = await fetch(`/api/v1/companies?search=${query}`);
    return response.json();
  },
  
  async getCompany(id: string): Promise<Company> {
    const response = await fetch(`/api/v1/companies/${id}`);
    return response.json();
  }
};
```

### API Configuration
```typescript
// src/config/api.ts
export const API_CONFIG = {
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
};
```

## 🧩 Components

### Form Components
```typescript
// src/components/forms/CompanyForm.tsx
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { companySchema } from '../schemas/companySchema';

export const CompanyForm = () => {
  const form = useForm({
    resolver: zodResolver(companySchema),
    defaultValues: {
      name: '',
      type: 'LTD',
      // ...
    }
  });

  // Form logic
};
```

### UI Components
```typescript
// src/components/ui/Button.tsx
import { cn } from '@/lib/utils';
import { cva } from 'class-variance-authority';

const buttonVariants = cva(
  'inline-flex items-center justify-center rounded-md text-sm font-medium',
  {
    variants: {
      variant: {
        default: 'bg-primary text-primary-foreground hover:bg-primary/90',
        destructive: 'bg-destructive text-destructive-foreground hover:bg-destructive/90',
        // ...
      }
    }
  }
);
```

## 🎯 Accessibility

### WCAG 2.1 AA Compliance
- **Keyboard Navigation**: All interactive elements
- **Screen Reader Support**: ARIA labels and descriptions
- **Color Contrast**: Minimum 4.5:1 ratio
- **Focus Management**: Visible focus indicators

### Accessibility Testing
```bash
# Install axe-core
npm install --save-dev @axe-core/react

# Run accessibility tests
npm run test:a11y
```

### Implementation
```typescript
// Semantic HTML
<button 
  aria-label="Search companies"
  aria-describedby="search-help"
  onClick={handleSearch}
>
  Search
</button>

// Focus management
import { useFocusRing } from '@react-aria/focus';

const { focusProps, isFocusVisible } = useFocusRing();
```

## 📱 Responsive Design

### Breakpoints
```javascript
// tailwind.config.js
module.exports = {
  theme: {
    screens: {
      'sm': '640px',
      'md': '768px',
      'lg': '1024px',
      'xl': '1280px',
      '2xl': '1536px',
    }
  }
};
```

### Mobile-First Approach
```typescript
// Responsive component
<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
  {/* Content */}
</div>
```

## 🌍 Internationalization

### Setup for Bilingual Support
```typescript
// src/i18n/index.ts
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

i18n
  .use(initReactI18next)
  .init({
    resources: {
      en: { translation: enTranslations },
      mi: { translation: miTranslations }
    },
    lng: 'en',
    fallbackLng: 'en'
  });
```

### Usage
```typescript
import { useTranslation } from 'react-i18next';

const { t } = useTranslation();
return <h1>{t('companies.title')}</h1>;
```

## 🔧 Configuration

### Environment Variables
```bash
# .env.local
VITE_API_BASE_URL=http://localhost:8080/api
VITE_ENVIRONMENT=development
VITE_FEATURE_GRAPHQL=true
VITE_FEATURE_ANALYTICS=false
```

### Build Configuration
```typescript
// vite.config.ts
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080'
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['react', 'react-dom'],
          ui: ['@radix-ui/react-dialog', '@radix-ui/react-dropdown-menu']
        }
      }
    }
  }
});
```

## 🚨 Troubleshooting

### Common Issues

1. **Development server won't start**:
   ```bash
   # Clear npm cache
   npm cache clean --force
   
   # Delete node_modules and reinstall
   rm -rf node_modules package-lock.json
   npm install
   ```

2. **Build fails with TypeScript errors**:
   ```bash
   # Check TypeScript configuration
   npm run typecheck
   
   # Fix type errors and rebuild
   npm run build
   ```

3. **Styling issues**:
   ```bash
   # Regenerate Tailwind styles
   npm run build:css
   
   # Check Tailwind configuration
   npx tailwindcss -i src/styles/index.css -o dist/output.css --watch
   ```

4. **API connection issues**:
   ```bash
   # Check if backend is running
   curl http://localhost:8080/api/actuator/health
   
   # Verify proxy configuration in vite.config.ts
   ```

### Development Tips

1. **Fast refresh**:
   ```bash
   # Vite provides automatic fast refresh
   npm run dev
   ```

2. **Debug mode**:
   ```typescript
   // Enable debug logging
   localStorage.setItem('debug', 'true');
   ```

3. **Component development**:
   ```bash
   # Use Storybook for component development
   npm run storybook
   ```

## 📚 Dependencies

### Core Dependencies
- **React**: 18.x
- **TypeScript**: 5.x
- **Vite**: 5.x
- **React Router**: 6.x

### UI Dependencies
- **TailwindCSS**: 3.x
- **ShadCN/UI**: Latest
- **Radix UI**: Components
- **Lucide React**: Icons

### Form Dependencies
- **React Hook Form**: 7.x
- **Zod**: Schema validation
- **@hookform/resolvers**: Form validation

### Development Dependencies
- **ESLint**: Code linting
- **Prettier**: Code formatting
- **Jest**: Testing framework
- **React Testing Library**: Component testing

## 🤝 Contributing

### Development Workflow
1. Create feature branch
2. Make changes following style guide
3. Add/update tests
4. Run `npm run lint && npm run typecheck`
5. Submit pull request

### Code Standards
- **TypeScript**: Strict mode enabled
- **React**: Functional components with hooks
- **Styling**: Utility-first with Tailwind
- **Testing**: Test user behavior, not implementation

### Pre-commit Checklist
- [ ] Tests pass: `npm test`
- [ ] TypeScript checks: `npm run typecheck`
- [ ] Linting passes: `npm run lint`
- [ ] Build succeeds: `npm run build`
- [ ] Accessibility tests pass: `npm run test:a11y`

---

**Need help?** Check the [main README](../README.md) or create an issue.