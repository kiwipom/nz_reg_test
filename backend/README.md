# Backend - NZ Companies Register

Kotlin/Spring Boot 3 backend service providing REST and GraphQL APIs for the NZ Companies Register system.

## 🏗️ Architecture

- **Framework**: Spring Boot 3.2.0
- **Language**: Kotlin 1.9.20
- **Database**: PostgreSQL with JPA/Hibernate
- **Build Tool**: Gradle with Kotlin DSL
- **Authentication**: OAuth2/JWT
- **API**: REST + GraphQL
- **Monitoring**: Actuator + OpenTelemetry

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Docker (for local PostgreSQL)

### Local Development

1. **Start local database**:
   ```bash
   # From project root
   docker-compose up -d postgres
   ```

2. **Run the application**:
   ```bash
   ./gradlew bootRun
   ```

3. **Run with specific profile**:
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=dev'
   ```

4. **Access the application**:
   - API: http://localhost:8080/api
   - Health Check: http://localhost:8080/api/actuator/health
   - OpenAPI Docs: http://localhost:8080/swagger-ui.html

## 📁 Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── kotlin/nz/govt/companiesoffice/register/
│   │   │   ├── config/          # Configuration classes
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── service/         # Business logic
│   │   │   ├── repository/      # Data access layer
│   │   │   ├── entity/          # JPA entities
│   │   │   ├── dto/             # Data transfer objects
│   │   │   ├── security/        # Security configuration
│   │   │   └── Application.kt   # Main application class
│   │   └── resources/
│   │       ├── application.yml  # Main configuration
│   │       └── db/migration/    # Flyway migrations
│   └── test/
│       └── kotlin/              # Test classes
├── build.gradle.kts             # Build configuration
├── gradle.properties            # Gradle properties
└── Dockerfile                   # Container configuration
```

## 🧪 Testing

### Run Tests
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "nz.govt.companiesoffice.register.service.CompanyServiceTest"

# Run with coverage
./gradlew test jacocoTestReport
```

### Test Coverage
- **Target**: 90%+ line coverage
- **Reports**: `build/reports/jacoco/test/html/index.html`

### Test Types
- **Unit Tests**: Service layer and utility functions
- **Integration Tests**: Repository layer with test database
- **Web Tests**: Controller layer with MockMvc

## 🔧 Development

### Code Quality
```bash
# Check code style
./gradlew ktlintCheck

# Fix code style issues
./gradlew ktlintFormat

# Run all quality checks
./gradlew check
```

### Build & Package
```bash
# Build JAR
./gradlew build

# Build Docker image
docker build -t nz-companies-register-backend .

# Clean build
./gradlew clean build
```

### Database Operations
```bash
# Run Flyway migrations
./gradlew flywayMigrate

# Check migration status
./gradlew flywayInfo

# Clean database (dev only)
./gradlew flywayClean
```

## 🗄️ Database

### Configuration
```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/nz_companies_register
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  flyway:
    baseline-on-migrate: true
```

### Migrations
- **Location**: `src/main/resources/db/migration/`
- **Naming**: `V1__Description.sql`
- **Tool**: Flyway for versioned migrations

### Local Database Access
```bash
# Connect to local database
psql -h localhost -U postgres -d nz_companies_register

# View database in Docker
docker exec -it nz-companies-postgres psql -U postgres -d nz_companies_register
```

## 🔐 Security

### Authentication
- **Type**: OAuth2/JWT
- **Provider**: AWS Cognito (production) / Mock (development)
- **Config**: `src/main/kotlin/security/SecurityConfig.kt`

### Authorization
- **Roles**: ADMIN, REGISTRAR, INTERNAL_OPS, PUBLIC
- **Method**: `@PreAuthorize` annotations
- **Audit**: All sensitive operations logged

### Development Security
```bash
# Disable security for local development
./gradlew bootRun --args='--spring.profiles.active=dev,no-security'
```

## 📊 Monitoring

### Actuator Endpoints
- **Health**: `/actuator/health`
- **Metrics**: `/actuator/metrics`
- **Info**: `/actuator/info`
- **Prometheus**: `/actuator/prometheus`

### Application Metrics
- **Custom Metrics**: Company registrations, search queries
- **Performance**: Response times, error rates
- **Business**: Annual return submissions, compliance rates

### Logging
```yaml
logging:
  level:
    nz.govt.companiesoffice: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

## 🌐 API Documentation

### REST API
- **Base URL**: `/api/v1`
- **OpenAPI**: http://localhost:8080/swagger-ui.html
- **Spec**: http://localhost:8080/v3/api-docs

### GraphQL
- **Endpoint**: `/graphql`
- **Playground**: http://localhost:8080/graphql (dev only)
- **Schema**: Auto-generated from resolvers

### Key Endpoints
```
GET    /api/v1/companies           # Search companies
POST   /api/v1/companies           # Register new company
GET    /api/v1/companies/{id}      # Get company details
PUT    /api/v1/companies/{id}      # Update company
GET    /api/v1/directors           # List directors
POST   /api/v1/annual-returns      # Submit annual return
```

## 🔧 Configuration

### Profiles
- **default**: Production configuration
- **dev**: Development with local database
- **test**: Testing with H2 database
- **docker**: Docker environment configuration

### Environment Variables
```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_USERNAME=postgres
DB_PASSWORD=postgres

# AWS Services
AWS_REGION=ap-southeast-2
S3_DOCUMENT_BUCKET=nz-companies-register-documents
DYNAMODB_ENDPOINT=http://localhost:8000

# Security
JWT_ISSUER_URI=https://nz-companies-register.auth0.com/
JWT_JWK_SET_URI=https://nz-companies-register.auth0.com/.well-known/jwks.json
```

### Application Properties
```yaml
# Custom application properties
app:
  business:
    annual-return:
      reminder-days: 30,7,0
    director:
      min-nz-au-residents: 1
    search:
      max-results: 100
      timeout-ms: 500
```

## 🚨 Troubleshooting

### Common Issues

1. **Database connection failed**:
   ```bash
   # Check if PostgreSQL is running
   docker-compose ps postgres
   
   # Restart database
   docker-compose restart postgres
   ```

2. **Flyway migration failed**:
   ```bash
   # Check migration history
   ./gradlew flywayInfo
   
   # Repair migration (if needed)
   ./gradlew flywayRepair
   ```

3. **Build fails with ktlint errors**:
   ```bash
   # Auto-fix formatting
   ./gradlew ktlintFormat
   
   # Then rebuild
   ./gradlew build
   ```

4. **Out of memory during build**:
   ```bash
   # Increase Gradle heap size
   export GRADLE_OPTS="-Xmx2g"
   ./gradlew build
   ```

### Development Tips

1. **Hot reload**:
   ```bash
   # Use Spring Boot DevTools
   ./gradlew bootRun --continuous
   ```

2. **Debug mode**:
   ```bash
   # Enable debug logging
   ./gradlew bootRun --args='--logging.level.nz.govt.companiesoffice=DEBUG'
   ```

3. **Profile-specific config**:
   ```bash
   # Use development profile
   ./gradlew bootRun --args='--spring.profiles.active=dev'
   ```

## 📚 Dependencies

### Core Dependencies
- **Spring Boot**: 3.2.0
- **Kotlin**: 1.9.20
- **PostgreSQL**: Latest
- **Flyway**: Database migrations

### AWS Dependencies
- **AWS SDK**: S3, DynamoDB, SNS, SQS
- **Spring Cloud AWS**: AWS integration

### Testing Dependencies
- **JUnit 5**: Testing framework
- **Mockk**: Kotlin mocking library
- **Testcontainers**: Integration testing
- **Spring Boot Test**: Web layer testing

### Build Tools
- **Gradle**: 8.5 with Kotlin DSL
- **ktlint**: Code formatting
- **JaCoCo**: Code coverage

## 🤝 Contributing

### Development Workflow
1. Create feature branch
2. Make changes following ktlint style
3. Add/update tests
4. Run `./gradlew check`
5. Submit pull request

### Code Standards
- **Kotlin**: Follow ktlint rules
- **Documentation**: KDoc for public APIs
- **Testing**: Write tests for new functionality
- **Logging**: Use structured logging

### Pre-commit Checklist
- [ ] Tests pass: `./gradlew test`
- [ ] Linting passes: `./gradlew ktlintCheck`
- [ ] Build succeeds: `./gradlew build`
- [ ] Security scan clean: `./gradlew dependencyCheckAnalyze`

---

**Need help?** Check the [main README](../README.md) or create an issue.