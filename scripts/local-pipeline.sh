#!/bin/bash

# Local CI/CD Pipeline Mimicking GitHub Actions
# This script replicates the GitHub Actions workflow for local development and testing

set -e  # Exit on any error

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
JAVA_VERSION="17"
NODE_VERSION="18"
PYTHON_VERSION="3.9"
POSTGRES_DB="nz_companies_register_test"
POSTGRES_USER="postgres"
POSTGRES_PASSWORD="postgres"
POSTGRES_PORT="5433"  # Use different port to avoid conflicts
POSTGRES_CONTAINER_NAME="nz-companies-postgres-test"

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Logging function
log() {
    local level=$1
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    case $level in
        "INFO")
            echo -e "${BLUE}[INFO]${NC} ${timestamp} - $message"
            ;;
        "SUCCESS")
            echo -e "${GREEN}[SUCCESS]${NC} ${timestamp} - $message"
            ;;
        "WARNING")
            echo -e "${YELLOW}[WARNING]${NC} ${timestamp} - $message"
            ;;
        "ERROR")
            echo -e "${RED}[ERROR]${NC} ${timestamp} - $message"
            ;;
    esac
}

# Check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check system requirements
check_requirements() {
    log "INFO" "Checking system requirements..."
    
    # Check Java
    if command_exists java; then
        JAVA_CURRENT=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
        if [ "$JAVA_CURRENT" -ge "$JAVA_VERSION" ]; then
            log "SUCCESS" "Java $JAVA_CURRENT found (required: $JAVA_VERSION)"
        else
            log "ERROR" "Java $JAVA_VERSION required, found $JAVA_CURRENT"
            exit 1
        fi
    else
        log "ERROR" "Java not found. Please install Java $JAVA_VERSION"
        exit 1
    fi
    
    # Check Node.js
    if command_exists node; then
        NODE_CURRENT=$(node --version | cut -d'v' -f2 | cut -d'.' -f1)
        if [ "$NODE_CURRENT" -ge "$NODE_VERSION" ]; then
            log "SUCCESS" "Node.js $NODE_CURRENT found (required: $NODE_VERSION)"
        else
            log "ERROR" "Node.js $NODE_VERSION required, found $NODE_CURRENT"
            exit 1
        fi
    else
        log "ERROR" "Node.js not found. Please install Node.js $NODE_VERSION"
        exit 1
    fi
    
    # Check Python
    if command_exists python3; then
        PYTHON_CURRENT=$(python3 --version | cut -d' ' -f2 | cut -d'.' -f1-2)
        log "SUCCESS" "Python $PYTHON_CURRENT found (required: $PYTHON_VERSION)"
    else
        log "ERROR" "Python3 not found. Please install Python $PYTHON_VERSION"
        exit 1
    fi
    
    # Check Docker (for PostgreSQL)
    if command_exists docker; then
        log "SUCCESS" "Docker found"
    else
        log "ERROR" "Docker not found. Please install Docker for PostgreSQL testing"
        exit 1
    fi
    
    # Check security tools
    if command_exists trivy; then
        log "SUCCESS" "Trivy found"
    else
        log "WARNING" "Trivy not found. Security scanning will be skipped"
    fi
}

# Check if PostgreSQL is already running via docker-compose
check_postgres_running() {
    if docker ps --filter "name=nz-companies-postgres" --filter "status=running" | grep -q "nz-companies-postgres"; then
        log "INFO" "PostgreSQL is already running via docker-compose"
        return 0
    else
        return 1
    fi
}

# Check if docker-compose services are available and offer to start them
check_docker_compose() {
    if [ -f "$PROJECT_ROOT/docker-compose.yml" ]; then
        log "INFO" "Found docker-compose.yml - checking if services are running..."
        if ! check_postgres_running; then
            log "INFO" "You can start the full development environment with: docker-compose up -d"
            log "INFO" "Or continue with isolated PostgreSQL container for testing"
        fi
    fi
}

# Start PostgreSQL for testing
start_postgres() {
    log "INFO" "Starting PostgreSQL container for testing..."
    
    # Check if main postgres is already running
    if check_postgres_running; then
        log "INFO" "Using existing PostgreSQL container from docker-compose"
        # Update config to use the existing postgres
        POSTGRES_PORT="5432"
        POSTGRES_DB="nz_companies_register"
        return 0
    fi
    
    # Stop existing test container if running
    docker stop $POSTGRES_CONTAINER_NAME 2>/dev/null || true
    docker rm $POSTGRES_CONTAINER_NAME 2>/dev/null || true
    
    # Start PostgreSQL container using the same image as docker-compose
    docker run -d \
        --name $POSTGRES_CONTAINER_NAME \
        -e POSTGRES_DB=$POSTGRES_DB \
        -e POSTGRES_USER=$POSTGRES_USER \
        -e POSTGRES_PASSWORD=$POSTGRES_PASSWORD \
        -p $POSTGRES_PORT:5432 \
        --health-cmd="pg_isready -U $POSTGRES_USER" \
        --health-interval=10s \
        --health-timeout=5s \
        --health-retries=5 \
        postgres:15-alpine
    
    # Wait for PostgreSQL to be ready
    log "INFO" "Waiting for PostgreSQL to be ready..."
    local retries=0
    local max_retries=30
    
    while [ $retries -lt $max_retries ]; do
        if docker exec $POSTGRES_CONTAINER_NAME pg_isready -U $POSTGRES_USER >/dev/null 2>&1; then
            log "SUCCESS" "PostgreSQL is ready"
            return 0
        fi
        retries=$((retries + 1))
        sleep 2
    done
    
    log "ERROR" "PostgreSQL failed to start after $max_retries attempts"
    exit 1
}

# Stop PostgreSQL
stop_postgres() {
    # Only stop if we started our own container
    if ! check_postgres_running; then
        log "INFO" "Stopping PostgreSQL test container..."
        docker stop $POSTGRES_CONTAINER_NAME 2>/dev/null || true
        docker rm $POSTGRES_CONTAINER_NAME 2>/dev/null || true
    else
        log "INFO" "Leaving docker-compose PostgreSQL container running"
    fi
}

# Backend tests
run_backend_tests() {
    log "INFO" "Running backend tests..."
    
    cd "$PROJECT_ROOT/backend"
    
    # Make gradlew executable
    chmod +x ./gradlew
    
    # Run linting
    log "INFO" "Running backend linting..."
    ./gradlew ktlintCheck
    log "SUCCESS" "Backend linting passed"
    
    # Build without tests first
    log "INFO" "Building backend..."
    ./gradlew build -x test
    log "SUCCESS" "Backend build completed"
    
    # Run tests
    log "INFO" "Running backend unit tests..."
    SPRING_PROFILES_ACTIVE=test \
    DB_HOST=localhost \
    DB_PORT=$POSTGRES_PORT \
    DB_NAME=$POSTGRES_DB \
    DB_USERNAME=$POSTGRES_USER \
    DB_PASSWORD=$POSTGRES_PASSWORD \
    ./gradlew test --info
    log "SUCCESS" "Backend tests passed"
    
    # Generate coverage report
    log "INFO" "Generating backend test coverage report..."
    ./gradlew jacocoTestReport
    
    # Check coverage (non-blocking like GitHub Actions)
    log "INFO" "Checking backend test coverage (non-blocking)..."
    ./gradlew jacocoTestCoverageVerification || {
        log "WARNING" "Coverage verification failed but continuing (matches GitHub Actions behavior)"
    }
    
    cd "$PROJECT_ROOT"
}

# Frontend tests
run_frontend_tests() {
    log "INFO" "Running frontend tests..."
    
    cd "$PROJECT_ROOT/frontend"
    
    # Install dependencies
    log "INFO" "Installing frontend dependencies..."
    npm ci
    log "SUCCESS" "Frontend dependencies installed"
    
    # Clean up coverage files first to avoid linting issues
    rm -rf coverage/ 2>/dev/null || true
    
    # Run linting
    log "INFO" "Running frontend linting..."
    npm run lint
    log "SUCCESS" "Frontend linting passed"
    
    # Run type checking
    log "INFO" "Running frontend type checking..."
    npm run typecheck
    log "SUCCESS" "Frontend type checking passed"
    
    # Run unit tests with coverage
    log "INFO" "Running frontend unit tests with coverage..."
    npm run test:coverage
    log "SUCCESS" "Frontend unit tests passed"
    
    # Run integration tests
    log "INFO" "Running frontend integration tests..."
    npm run test:integration
    log "SUCCESS" "Frontend integration tests passed"
    
    # Build frontend
    log "INFO" "Building frontend..."
    npm run build
    log "SUCCESS" "Frontend build completed"
    
    cd "$PROJECT_ROOT"
}

# Infrastructure tests
run_infrastructure_tests() {
    log "INFO" "Running infrastructure tests..."
    
    cd "$PROJECT_ROOT/infrastructure/cdk"
    
    # Install dependencies
    log "INFO" "Installing infrastructure dependencies..."
    pip install -r requirements.txt
    pip install -r test_requirements.txt
    log "SUCCESS" "Infrastructure dependencies installed"
    
    # Run linting
    log "INFO" "Running infrastructure linting..."
    black --check .
    flake8 .
    log "SUCCESS" "Infrastructure linting passed"
    
    # Run unit tests with coverage
    log "INFO" "Running infrastructure unit tests with coverage..."
    python -m pytest tests/ -v --cov=. --cov-report=xml --cov-report=term
    log "SUCCESS" "Infrastructure tests passed"
    
    cd "$PROJECT_ROOT"
}

# Security scanning
run_security_scan() {
    log "INFO" "Running security scans..."
    
    # Trivy vulnerability scan
    if command_exists trivy; then
        log "INFO" "Running Trivy vulnerability scan..."
        trivy fs . --format table --exit-code 1 --severity HIGH,CRITICAL || {
            log "WARNING" "Trivy found HIGH or CRITICAL vulnerabilities"
            # Don't exit - let it be a warning for local development
        }
        log "SUCCESS" "Trivy scan completed"
    else
        log "WARNING" "Trivy not found - skipping vulnerability scan"
    fi
    
    # Bandit security scan for Python
    if command_exists bandit; then
        log "INFO" "Running Bandit security scan on Python code..."
        bandit -r infrastructure/cdk/stacks/ || {
            log "WARNING" "Bandit found potential security issues"
        }
        log "SUCCESS" "Bandit scan completed"
    else
        log "WARNING" "Bandit not found - skipping Python security scan"
    fi
    
    # Additional security checks can be added here
    log "SUCCESS" "Security scanning completed"
}

# Build validation
run_build_validation() {
    log "INFO" "Running build validation..."
    
    # Backend build
    log "INFO" "Validating backend build..."
    cd "$PROJECT_ROOT/backend"
    ./gradlew build -x test
    log "SUCCESS" "Backend build validation passed"
    
    # Frontend build
    log "INFO" "Validating frontend build..."
    cd "$PROJECT_ROOT/frontend"
    npm run build
    log "SUCCESS" "Frontend build validation passed"
    
    cd "$PROJECT_ROOT"
}

# Cleanup function
cleanup() {
    log "INFO" "Cleaning up..."
    stop_postgres
    log "SUCCESS" "Cleanup completed"
}

# Main execution
main() {
    log "INFO" "Starting local CI/CD pipeline..."
    
    # Parse command line arguments
    SKIP_POSTGRES=false
    SKIP_SECURITY=false
    SKIP_TESTS=false
    SKIP_BUILD=false
    USE_DOCKER_COMPOSE=false
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            --skip-postgres)
                SKIP_POSTGRES=true
                shift
                ;;
            --skip-security)
                SKIP_SECURITY=true
                shift
                ;;
            --skip-tests)
                SKIP_TESTS=true
                shift
                ;;
            --skip-build)
                SKIP_BUILD=true
                shift
                ;;
            --use-docker-compose)
                USE_DOCKER_COMPOSE=true
                shift
                ;;
            --help|-h)
                echo "Usage: $0 [OPTIONS]"
                echo "Options:"
                echo "  --skip-postgres       Skip PostgreSQL setup"
                echo "  --skip-security       Skip security scanning"
                echo "  --skip-tests          Skip all tests"
                echo "  --skip-build          Skip build validation"
                echo "  --use-docker-compose  Use docker-compose to start services"
                echo "  --help, -h            Show this help message"
                exit 0
                ;;
            *)
                log "ERROR" "Unknown option: $1"
                exit 1
                ;;
        esac
    done
    
    # Set trap for cleanup
    trap cleanup EXIT
    
    # Check system requirements
    check_requirements
    
    # Check docker-compose setup
    check_docker_compose
    
    # Start PostgreSQL if not skipped
    if [ "$SKIP_POSTGRES" = false ]; then
        if [ "$USE_DOCKER_COMPOSE" = true ]; then
            log "INFO" "Starting services with docker-compose..."
            cd "$PROJECT_ROOT"
            docker-compose up -d postgres
            # Wait for PostgreSQL to be ready
            log "INFO" "Waiting for PostgreSQL to be ready..."
            sleep 15
            POSTGRES_PORT="5432"
            POSTGRES_DB="nz_companies_register"
        else
            start_postgres
        fi
    fi
    
    # Run tests if not skipped
    if [ "$SKIP_TESTS" = false ]; then
        run_backend_tests
        run_frontend_tests
        run_infrastructure_tests
    fi
    
    # Run security scan if not skipped
    if [ "$SKIP_SECURITY" = false ]; then
        run_security_scan
    fi
    
    # Run build validation if not skipped
    if [ "$SKIP_BUILD" = false ]; then
        run_build_validation
    fi
    
    log "SUCCESS" "Local CI/CD pipeline completed successfully!"
}

# Check if script is being sourced or executed
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi