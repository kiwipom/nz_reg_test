# NZ Companies Register System

A modern, cloud-native application built to comply with the Companies Act 1993, providing secure company registration, compliance management, and public record access.

## 🏗️ Architecture

- **Backend**: Kotlin + Spring Boot 3 (REST + GraphQL APIs)
- **Frontend**: React + TypeScript + TailwindCSS + ShadCN/UI
- **Database**: PostgreSQL (Aurora) + DynamoDB + S3
- **Infrastructure**: AWS EKS + CDK (Python)
- **Monitoring**: CloudWatch + Prometheus + Grafana + Jaeger

## 🚀 Quick Start

### Prerequisites

- Docker & Docker Compose
- Node.js 18+ & npm
- Java 17+ & Gradle
- Python 3.9+ & pip (for CDK)
- **Auth0 account** (for authentication)

### Auth0 Setup

**IMPORTANT**: Before running the application, you must set up Auth0 for authentication:

1. **Follow the comprehensive Auth0 setup guide**: [docs/AUTH0_SETUP.md](docs/AUTH0_SETUP.md)
2. **Create Auth0 tenant and application**
3. **Configure API and roles**
4. **Set up environment variables**

### Local Development

1. **Clone and setup**:
   ```bash
   git clone <repository-url>
   cd nz_reg_test
   ```

2. **Configure environment variables**:
   ```bash
   # Backend
   cd backend
   cp .env.example .env
   # Edit .env with your Auth0 configuration
   
   # Frontend
   cd ../frontend
   cp .env.example .env
   # Edit .env with your Auth0 configuration
   ```

3. **Start local infrastructure**:
   ```bash
   docker-compose up -d
   ```
   This starts PostgreSQL, DynamoDB Local, LocalStack (S3/SNS/SQS), and monitoring stack.

4. **Run backend**:
   ```bash
   cd backend
   ./gradlew bootRun
   ```
   Backend will be available at http://localhost:8080

5. **Run frontend**:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   Frontend will be available at http://localhost:5173

### Development Services

| Service | URL | Description |
|---------|-----|-------------|
| Frontend | http://localhost:5173 | React application |
| Backend API | http://localhost:8080/api | Spring Boot REST API |
| API Docs | http://localhost:8080/swagger-ui.html | OpenAPI documentation |
| GraphQL | http://localhost:8080/graphql | GraphQL playground |
| Grafana | http://localhost:3001 | Monitoring dashboards |
| Prometheus | http://localhost:9090 | Metrics collection |
| Jaeger | http://localhost:16686 | Distributed tracing |

## 📁 Project Structure

```
nz_reg_test/
├── backend/           # Kotlin/Spring Boot 3 application
├── frontend/          # React/TypeScript application
├── infrastructure/    # AWS CDK infrastructure code
├── docs/             # Architecture diagrams and documentation
├── docker-compose.yml # Local development environment
├── CLAUDE.md         # AI assistant prompts and context
├── REQUIREMENTS.md   # System requirements specification
└── TODO.md          # Development roadmap and progress
```

## 🧪 Testing

### Run All Tests
```bash
# Backend tests
cd backend && ./gradlew test

# Frontend tests
cd frontend && npm test

# Infrastructure tests
cd infrastructure/cdk && make test
```

### Test Coverage
- **Backend**: 90%+ target (JaCoCo reports)
- **Frontend**: 80%+ target (Jest/React Testing Library)
- **Infrastructure**: 90%+ target (pytest)

## 🔧 Development Workflow

### 1. Local Development
```bash
# Start infrastructure
docker-compose up -d

# Backend development
cd backend
./gradlew bootRun --continuous

# Frontend development
cd frontend
npm run dev
```

### 2. Code Quality
```bash
# Backend linting
cd backend && ./gradlew ktlintCheck

# Frontend linting
cd frontend && npm run lint

# Format code
cd backend && ./gradlew ktlintFormat
cd frontend && npm run format
```

### 3. Build & Package
```bash
# Build backend
cd backend && ./gradlew build

# Build frontend
cd frontend && npm run build

# Build Docker images
docker-compose build
```

## 🏗️ Infrastructure

### CDK Development
```bash
cd infrastructure/cdk

# Install dependencies
make install

# Run tests
make test

# Synthesize CloudFormation
make synth

# Deploy (when ready)
make deploy
```

### Local AWS Services (LocalStack)
- **S3**: http://localhost:4566
- **DynamoDB**: http://localhost:8000
- **SNS/SQS**: http://localhost:4566

## 📊 Monitoring

### Development Monitoring
- **Application Logs**: `docker-compose logs -f backend`
- **Database Logs**: `docker-compose logs -f postgres`
- **Metrics**: http://localhost:3001 (Grafana)
- **Tracing**: http://localhost:16686 (Jaeger)

### Production Monitoring
- **CloudWatch**: Logs, metrics, and alarms
- **Prometheus**: Application metrics
- **Grafana**: Custom dashboards
- **Jaeger**: Distributed tracing

## 🔐 Security

### Development Security
- **Database**: postgres/postgres (local only)
- **Authentication**: OAuth2/JWT via Auth0
- **Encryption**: Disabled for local development
- **RBAC**: Role-based access control (Admin, Registrar, Internal Ops, Public)

### Production Security
- **Authentication**: OAuth2/JWT via Auth0
- **Encryption**: KMS encryption at rest and in transit
- **Network**: VPC with private subnets
- **Secrets**: AWS Secrets Manager
- **Audit Logging**: Comprehensive audit trail for all actions

## 📖 Documentation

- **Architecture**: [docs/architecture-overview.md](docs/architecture-overview.md)
- **API Documentation**: Available at `/swagger-ui.html` when running
- **System Requirements**: [REQUIREMENTS.md](REQUIREMENTS.md)
- **Development Progress**: [TODO.md](TODO.md)

## 🤝 Contributing

### Development Setup
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests and linting
5. Submit a pull request

### Code Standards
- **Kotlin**: Follow ktlint rules
- **TypeScript**: Follow ESLint + Prettier configuration
- **Python**: Follow PEP 8 with Black formatter
- **Commits**: Use conventional commit format

### Pre-commit Checklist
- [ ] Tests pass (`make test`)
- [ ] Linting passes (`make lint`)
- [ ] Documentation updated
- [ ] Security scan clean (`make security`)

## 📋 Environment Variables

### Backend
```bash
# Database
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Auth0
AUTH0_DOMAIN=your-tenant.au.auth0.com
AUTH0_AUDIENCE=https://api.companies-register.govt.nz
AUTH0_ISSUER=https://your-tenant.au.auth0.com/

# AWS Services
AWS_REGION=ap-southeast-2
S3_DOCUMENT_BUCKET=nz-companies-register-documents
DYNAMODB_ENDPOINT=http://localhost:8000
```

### Frontend
```bash
# API Configuration
VITE_API_BASE_URL=http://localhost:8080/api

# Auth0
VITE_AUTH0_DOMAIN=your-tenant.au.auth0.com
VITE_AUTH0_CLIENT_ID=your-client-id
VITE_AUTH0_AUDIENCE=https://api.companies-register.govt.nz
VITE_AUTH0_REDIRECT_URI=http://localhost:3000/callback
```

## 🚨 Troubleshooting

### Common Issues

1. **Docker containers not starting**:
   ```bash
   docker-compose down
   docker-compose up -d --build
   ```

2. **Database connection issues**:
   ```bash
   docker-compose restart postgres
   ```

3. **Frontend not loading**:
   ```bash
   cd frontend
   rm -rf node_modules package-lock.json
   npm install
   ```

4. **Backend build issues**:
   ```bash
   cd backend
   ./gradlew clean build
   ```

### Development Support
- **Logs**: `docker-compose logs -f [service]`
- **Database**: Connect to postgres://postgres:postgres@localhost:5432/nz_companies_register
- **Health Check**: http://localhost:8080/api/actuator/health

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🏛️ Compliance

This system is designed to comply with:
- **Companies Act 1993** (New Zealand)
- **Privacy Act 2020** (New Zealand)
- **WCAG 2.1 AA** accessibility standards
- **ISO 27001** security standards

---

**Need help?** Check the [troubleshooting guide](docs/troubleshooting.md) or create an issue.