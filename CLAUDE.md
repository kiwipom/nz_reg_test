# CLAUDE.md - NZ Companies Register System

## Testing Commands
```bash
# Backend (Kotlin/Spring Boot)
./gradlew test
./gradlew build
./gradlew ktlintCheck

# Frontend (React/TypeScript)
npm test
npm run build
npm run lint
npm run typecheck
```

## System Context

This is a comprehensive NZ Companies Register System built to comply with the Companies Act 1993. The system manages company registrations, maintains public records, enforces compliance obligations, and supports legal processes.

**Key Architecture:**
- Backend: Kotlin + Spring Boot 3 (modular monolith)
- Frontend: React + TypeScript + TailwindCSS + ShadCN/UI
- Database: PostgreSQL (transactional) + DynamoDB/S3 (documents)
- Infrastructure: AWS EKS + Docker
- Messaging: SNS/SQS for event-driven features

## Core Prompts

### /backend-feature
You are developing a backend feature for the NZ Companies Register System using Kotlin and Spring Boot 3.

**Context:**
- Follow Spring Boot 3 best practices and conventions
- Use Spring Data JPA for persistence with PostgreSQL
- Implement proper service layer separation from API controllers
- Use OAuth2/JWT for authentication (Auth0 or AWS Cognito)
- All operations must maintain audit logs for compliance
- Follow event-driven patterns using SNS/SQS for notifications
- Ensure compliance with Companies Act 1993 requirements

**Key Requirements:**
- Implement proper validation and error handling
- Add comprehensive logging (structured JSON)
- Include unit tests (90%+ coverage target)
- Follow immutable snapshot principles for historical data
- Implement proper RBAC (Admin, Public, Registrar, Internal Ops)

### /frontend-feature
You are developing a frontend feature for the NZ Companies Register System using React and TypeScript.

**Context:**
- Use React + TypeScript with TailwindCSS and ShadCN/UI components
- Forms should use Zod for validation and React Hook Form
- State management with Zustand or Redux Toolkit
- All interfaces must be WCAG 2.1 AA compliant
- Prepare for bilingual support (English/Mori) within 18 months
- Performance targets: Search < 500ms, Public views < 300ms

**Key Requirements:**
- Implement proper error boundaries and loading states
- Add comprehensive TypeScript types
- Include unit and integration tests
- Follow responsive design principles
- Implement proper accessibility features
- Use proper form validation and user feedback

### /database-schema
You are designing database schemas for the NZ Companies Register System.

**Context:**
- Primary database: PostgreSQL for transactional data
- Document storage: S3 + DynamoDB for filings and attachments
- Must support immutable historical snapshots per section 89 of Companies Act
- Implement proper indexing for search performance
- Support multi-AZ deployment patterns

**Key Requirements:**
- Design normalized schemas with proper relationships
- Include audit trails and versioning
- Implement proper constraints and validations
- Consider read replica patterns for search optimization
- Design for horizontal scaling where needed

### /api-design
You are designing REST and GraphQL APIs for the NZ Companies Register System.

**Context:**
- Implement both REST and GraphQL endpoints
- Use OpenAPI specification for documentation
- Authentication via OAuth2/JWT
- Rate limiting and throttling via API Gateway
- Public APIs must be accessible and well-documented

**Key Requirements:**
- Design RESTful endpoints following HTTP conventions
- Implement proper error responses and status codes
- Add comprehensive API documentation
- Include input validation and sanitization
- Design for both public and internal API consumers

### /compliance-feature
You are implementing a compliance feature for the NZ Companies Register System.

**Context:**
- Must comply with Companies Act 1993 requirements
- Implement proper workflow management for annual returns
- Enforce director residency requirements (NZ/Australian resident)
- Track compliance obligations and deadlines
- Support strike-off procedures for non-compliance

**Key Requirements:**
- Implement automated reminder systems (1 month, 1 week, due date, post-due)
- Track and enforce director consent requirements
- Maintain proper audit trails for all compliance actions
- Support registrar override capabilities
- Implement proper escalation workflows

### /search-feature
You are implementing search functionality for the NZ Companies Register System.

**Context:**
- Public search interface for company records
- Search by name, company number, director, or shareholder
- Support for historical company extracts at specific dates
- Performance target: < 500ms for search queries
- Consider CQRS pattern for read optimization

**Key Requirements:**
- Implement full-text search capabilities
- Support advanced filtering and sorting
- Add proper pagination for large result sets
- Include search result highlighting
- Implement search analytics and monitoring

### /notification-system
You are implementing the notification system for the NZ Companies Register System.

**Context:**
- Event-driven architecture using SNS/SQS or Kafka
- Handle annual return reminders and compliance notifications
- Support multiple notification channels (email, SMS, postal)
- Integrate with external services (NZ Post for addressing)

**Key Requirements:**
- Implement reliable message delivery patterns
- Add proper retry mechanisms and dead letter queues
- Support notification templating and personalization
- Include delivery tracking and status updates
- Implement proper error handling and monitoring

### /security-implementation
You are implementing security features for the NZ Companies Register System.

**Context:**
- Full encryption at rest and in transit
- Role-based access control (RBAC)
- Audit logging for all sensitive operations
- Comply with NZ privacy and data retention laws
- Support secure document uploads and storage

**Key Requirements:**
- Implement proper authentication and authorization
- Add comprehensive audit logging
- Secure file upload and validation
- Implement proper session management
- Add security headers and CORS configuration

### /monitoring-observability
You are implementing monitoring and observability for the NZ Companies Register System.

**Context:**
- Structured JSON logging via FluentBit � CloudWatch
- Metrics collection with Prometheus + Grafana
- Distributed tracing with OpenTelemetry + Jaeger
- Alerting integration with PagerDuty
- SLA target: 99.9% uptime

**Key Requirements:**
- Implement comprehensive application logging
- Add business metrics and KPIs
- Set up proper alerting thresholds
- Include performance monitoring
- Implement health checks and readiness probes

### /deployment-infrastructure
You are working with deployment and infrastructure for the NZ Companies Register System.

**Context:**
- AWS EKS with Docker containerization
- Multi-AZ deployment for high availability
- CI/CD with GitHub Actions � ECR � EKS
- Infrastructure as Code (Terraform/CloudFormation)
- Single-tenant Kubernetes namespaces

**Key Requirements:**
- Implement proper container orchestration
- Add auto-scaling configurations (HPA)
- Set up secure networking and load balancing
- Implement proper secrets management
- Add disaster recovery procedures

### /testing-strategy
You are implementing testing strategies for the NZ Companies Register System.

**Context:**
- Target coverage: Unit 90%+, Integration 80%+
- E2E testing for public flows
- Performance testing for search and public access
- Security testing for all endpoints
- Compliance testing for regulatory requirements

**Key Requirements:**
- Implement comprehensive unit test suites
- Add integration tests for critical workflows
- Include E2E tests for user journeys
- Add performance and load testing
- Implement security and compliance testing

## Development Guidelines

1. **Code Quality**: Follow established patterns and conventions for both Kotlin and TypeScript
2. **Documentation**: Maintain comprehensive API documentation and ADRs
3. **Security**: Always consider security implications and compliance requirements
4. **Performance**: Monitor and optimize for specified performance targets
5. **Testing**: Maintain high test coverage with meaningful test cases
6. **Accessibility**: Ensure all UI components meet WCAG 2.1 AA standards
7. **Scalability**: Design for horizontal scaling and high availability
8. **Monitoring**: Include proper logging, metrics, and alerting in all features

## Development Workflow - for each Phase Section in TODO.md:**

### Before the first task:
1. **Create a new feature branch** - Named to match the scope of the section, e.g. `feature/phase-1.4-authentication-security`

### Standard Development Cycle:
1. **Analyze the task** - Review the `[ ] Item` in TODO.md and understand requirements
2. **Write the code** - Implement the feature following the appropriate `/prompt` guidelines
3. **Write comprehensive tests** - Unit tests, integration tests, and any E2E tests needed. Consider happy path and negative test cases.
4. **Verify all tests pass** - Run `npm run test:run` (frontend) or `./gradlew test` (backend)
5. **Run linting and type checking** - `npm run lint && npm run typecheck` or `./gradlew ktlintCheck`
6. **Create a commit** - Clear, descriptive commit message following conventional commits
7. **Update TODO.md** - Change `[ ] Item` to `[x] Item` in the appropriate section. This means writing changes into the repo root /TODO.md file - not just tracking the `Update Todos` list in the Claude terminal
8. **Move to next item** - Repeat cycle for the next uncompleted task

### On completion of the final task within the section:
1. **Prepare the branch** - Run `/scripts/local-pipeline.sh` - do not push the branch unless GREEN!
2. **Create the PR** - push the branch and create a PR
3. **Update TODO.md** - Update overall section ✅ Complete, and update overall %age progress

### Commit Message Format:
```
<type>: <description>

- <specific change 1>
- <specific change 2>
- <test coverage details>

🤖 Generated with [Claude Code](https://claude.ai/code)

Co-Authored-By: Claude <noreply@anthropic.com>
```

## Quick Reference

- **Legal Framework**: Companies Act 1993
- **Primary Database**: PostgreSQL (Aurora)
- **Document Storage**: S3 + DynamoDB
- **Authentication**: OAuth2/JWT via Auth0 or AWS Cognito
- **Messaging**: SNS/SQS for event-driven features
- **Frontend**: React + TypeScript + TailwindCSS + ShadCN/UI
- **Backend**: Kotlin + Spring Boot 3
- **Container Platform**: AWS EKS
- **Monitoring**: Prometheus + Grafana + OpenTelemetry