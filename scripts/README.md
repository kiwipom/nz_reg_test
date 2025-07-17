# Local CI/CD Pipeline Scripts

This directory contains scripts to replicate the GitHub Actions CI/CD pipeline locally for development and testing.

## local-pipeline.sh

A comprehensive script that mimics the GitHub Actions workflow (`/.github/workflows/ci.yml`) for local development.

### Features

- **Backend Testing**: Kotlin/Spring Boot linting, building, and testing
- **Frontend Testing**: React/TypeScript linting, type checking, unit tests, and integration tests
- **Infrastructure Testing**: Python CDK linting and unit tests
- **Security Scanning**: Trivy vulnerability scanning and Bandit security analysis
- **Build Validation**: Complete build verification for all components
- **PostgreSQL Setup**: Automatic Docker-based PostgreSQL setup for testing

### Prerequisites

- Java 17+
- Node.js 18+
- Python 3.9+
- Docker (for PostgreSQL)
- Optional: Trivy (for security scanning)
- Optional: Bandit (for Python security scanning)

### Usage

```bash
# Run full pipeline
./scripts/local-pipeline.sh

# Skip PostgreSQL setup (if you have a local instance)
./scripts/local-pipeline.sh --skip-postgres

# Skip security scanning
./scripts/local-pipeline.sh --skip-security

# Skip all tests (build validation only)
./scripts/local-pipeline.sh --skip-tests

# Skip build validation
./scripts/local-pipeline.sh --skip-build

# Show help
./scripts/local-pipeline.sh --help
```

### Pipeline Stages

1. **System Requirements Check**
   - Verifies Java, Node.js, Python, and Docker installations
   - Checks for optional security tools

2. **PostgreSQL Setup**
   - Starts PostgreSQL container for testing
   - Configures test database

3. **Backend Tests**
   - Kotlin linting (ktlint)
   - Gradle build
   - Unit tests (currently disabled due to compilation issues)
   - Coverage reporting (when tests are enabled)

4. **Frontend Tests**
   - ESLint linting
   - TypeScript type checking
   - Unit tests with coverage
   - Integration tests
   - Build validation

5. **Infrastructure Tests**
   - Python code formatting (black)
   - Linting (flake8)
   - Unit tests with coverage

6. **Security Scanning**
   - Trivy vulnerability scanning
   - Bandit Python security analysis

7. **Build Validation**
   - Final build verification for all components

### Output

The script provides colored output with timestamps:
- **INFO** (Blue): General information
- **SUCCESS** (Green): Successful operations
- **WARNING** (Yellow): Non-critical issues
- **ERROR** (Red): Critical failures

### Integration with Development Workflow

This script is designed to catch issues before pushing to GitHub, reducing failed PR builds. Run it:

- Before creating pull requests
- After making significant changes
- As part of your local development routine

### Troubleshooting

1. **PostgreSQL Issues**: Ensure Docker is running and port 5432 is available
2. **Backend Test Issues**: Currently disabled in CI due to compilation problems
3. **Security Scan Warnings**: Review and address high/critical vulnerabilities
4. **Build Failures**: Check individual component logs for specific issues

### Notes

- The script automatically cleans up resources (PostgreSQL container) on exit
- Security scanning failures are treated as warnings in local development
- Backend unit tests are currently disabled to match CI configuration
- All paths and configurations match the GitHub Actions workflow