# NZ Companies Register System - Development TODO

## Phase 1: Foundation & Infrastructure (Weeks 1-4)

### 1.1 Project Setup
- [ ] Initialize Kotlin/Spring Boot 3 backend project structure
- [ ] Set up React/TypeScript frontend project with Vite
- [ ] Configure build tools (Gradle, npm/yarn, Docker)
- [ ] Set up code quality tools (ktlint, ESLint, Prettier)
- [ ] Initialize Git repository with proper .gitignore

### 1.2 Infrastructure Foundation
- [ ] Set up AWS account and basic security configuration
- [ ] Configure PostgreSQL database (Aurora) with multi-AZ
- [ ] Set up S3 buckets for document storage
- [ ] Configure DynamoDB for document metadata
- [ ] Set up basic EKS cluster configuration
- [ ] Configure container registry (ECR)

### 1.3 Core Architecture Setup
- [ ] Implement basic Spring Boot application structure
- [ ] Set up database migrations with Flyway
- [ ] Configure JPA entities and repositories
- [ ] Set up React project with TailwindCSS and ShadCN/UI
- [ ] Configure routing and basic layout components
- [ ] Set up state management (Zustand/Redux Toolkit)

### 1.4 Authentication & Security
- [ ] Implement OAuth2/JWT authentication (Auth0 or AWS Cognito)
- [ ] Set up RBAC system (Admin, Public, Registrar, Internal Ops)
- [ ] Configure CORS and security headers
- [ ] Implement audit logging framework
- [ ] Set up encryption for sensitive data

## Phase 2: Core Data Models & APIs (Weeks 5-8)

### 2.1 Database Schema Design
- [ ] Design Company entity and related tables
- [ ] Create Director entity with residency tracking
- [ ] Design Shareholder and share allocation tables
- [ ] Implement Address entities (registered, service, communication)
- [ ] Create audit trail and versioning tables
- [ ] Design document storage schema in DynamoDB

### 2.2 Core Business Logic
- [ ] Implement Company service layer
- [ ] Create Director management service
- [ ] Develop Shareholder management service
- [ ] Build Address validation and management
- [ ] Implement document upload and storage service
- [ ] Create audit service for compliance tracking

### 2.3 REST API Development
- [ ] Design and implement Company REST endpoints
- [ ] Create Director management APIs
- [ ] Build Shareholder management APIs
- [ ] Implement document upload/download APIs
- [ ] Create search APIs with filtering
- [ ] Generate OpenAPI documentation

### 2.4 GraphQL API (Optional)
- [ ] Set up GraphQL schema
- [ ] Implement Company queries and mutations
- [ ] Create Director and Shareholder resolvers
- [ ] Build search and filtering capabilities
- [ ] Set up GraphQL playground

## Phase 3: Company Registration & Management (Weeks 9-12)

### 3.1 Company Registration Flow
- [ ] Build company name availability check
- [ ] Implement company registration form (frontend)
- [ ] Create company registration API endpoint
- [ ] Add share capital and allocation handling
- [ ] Implement constitution file upload
- [ ] Build registration confirmation workflow

### 3.2 Director Management
- [ ] Create director appointment form
- [ ] Implement director consent capture and storage
- [ ] Build director resignation workflow
- [ ] Add NZ/Australian residency validation
- [ ] Implement director disqualification checks
- [ ] Create director update notifications

### 3.3 Shareholder Management
- [ ] Build shareholder registration forms
- [ ] Implement share transfer workflows
- [ ] Create share allotment tracking
- [ ] Build shareholder update notifications
- [ ] Implement share class and rights management

### 3.4 Address Management
- [ ] Create address validation service
- [ ] Build address update workflows
- [ ] Implement address history tracking
- [ ] Integrate with NZ Post address services
- [ ] Create address change notifications

## Phase 4: Compliance & Workflow Management (Weeks 13-16)

### 4.1 Annual Return System
- [ ] Design annual return data model
- [ ] Create annual return form (frontend)
- [ ] Build annual return submission API
- [ ] Implement return status tracking
- [ ] Create annual return history view

### 4.2 Notification System
- [ ] Set up SNS/SQS messaging infrastructure
- [ ] Build email notification service
- [ ] Create SMS notification capability
- [ ] Implement postal notification integration
- [ ] Build notification templates and personalization

### 4.3 Reminder Workflows
- [ ] Create reminder scheduling service
- [ ] Implement 1-month before due date reminders
- [ ] Build 1-week before due date reminders
- [ ] Create due date notifications
- [ ] Implement post-due follow-up (7 days, 30 days)

### 4.4 Compliance Enforcement
- [ ] Build strike-off workflow for overdue returns
- [ ] Implement director residency compliance checks
- [ ] Create compliance dashboard for registrars
- [ ] Build manual override capabilities
- [ ] Implement escalation procedures

## Phase 5: Public Search & Records (Weeks 17-20)

### 5.1 Search Infrastructure
- [ ] Implement full-text search capabilities
- [ ] Build search indexing service
- [ ] Create search result ranking algorithm
- [ ] Implement search filters and sorting
- [ ] Add search result pagination

### 5.2 Public Search Interface
- [ ] Design public search UI
- [ ] Build company name search
- [ ] Implement company number search
- [ ] Create director name search
- [ ] Build shareholder search (where permitted)
- [ ] Add advanced search filters

### 5.3 Public Records Display
- [ ] Create company profile page
- [ ] Build director listing display
- [ ] Implement shareholder information display
- [ ] Create document download interface
- [ ] Build company history timeline

### 5.4 Historical Snapshots
- [ ] Implement immutable snapshot creation
- [ ] Build snapshot storage service
- [ ] Create historical record retrieval API
- [ ] Build point-in-time company extract
- [ ] Implement snapshot scheduling

## Phase 6: Advanced Features (Weeks 21-24)

### 6.1 Company Lifecycle Management
- [ ] Implement company name change workflow
- [ ] Build company amalgamation process
- [ ] Create liquidation workflow
- [ ] Implement company removal process
- [ ] Build company restoration procedures

### 6.2 Document Management
- [ ] Create document classification system
- [ ] Build document versioning
- [ ] Implement document approval workflows
- [ ] Create document retention policies
- [ ] Build document search and retrieval

### 6.3 Integration Layer
- [ ] Build NZBN integration (future)
- [ ] Create external API connectors
- [ ] Implement data synchronization
- [ ] Build webhook support
- [ ] Create API rate limiting

### 6.4 Reporting & Analytics
- [ ] Build compliance reporting dashboard
- [ ] Create business intelligence queries
- [ ] Implement performance metrics
- [ ] Build audit trail reporting
- [ ] Create user activity analytics

## Phase 7: Monitoring & Observability (Weeks 25-26)

### 7.1 Application Monitoring
- [ ] Set up structured JSON logging
- [ ] Configure FluentBit ’ CloudWatch
- [ ] Implement application metrics
- [ ] Set up Prometheus + Grafana
- [ ] Configure alerting with PagerDuty

### 7.2 Infrastructure Monitoring
- [ ] Set up EKS cluster monitoring
- [ ] Configure database monitoring
- [ ] Implement S3 and DynamoDB metrics
- [ ] Set up network monitoring
- [ ] Create infrastructure dashboards

### 7.3 Performance Monitoring
- [ ] Implement OpenTelemetry tracing
- [ ] Set up Jaeger for distributed tracing
- [ ] Configure performance baselines
- [ ] Create performance alerting
- [ ] Build performance optimization reports

### 7.4 Security Monitoring
- [ ] Set up security event logging
- [ ] Configure access monitoring
- [ ] Implement anomaly detection
- [ ] Create security dashboards
- [ ] Set up compliance monitoring

## Phase 8: Testing & Quality Assurance (Weeks 27-30)

### 8.1 Unit Testing
- [ ] Achieve 90%+ backend unit test coverage
- [ ] Implement comprehensive frontend unit tests
- [ ] Create mock data generators
- [ ] Set up test automation in CI/CD
- [ ] Build test reporting dashboards

### 8.2 Integration Testing
- [ ] Achieve 80%+ integration test coverage
- [ ] Create API integration tests
- [ ] Build database integration tests
- [ ] Test external service integrations
- [ ] Implement contract testing

### 8.3 End-to-End Testing
- [ ] Create user journey tests
- [ ] Build registration flow E2E tests
- [ ] Test search functionality E2E
- [ ] Create compliance workflow tests
- [ ] Implement accessibility testing

### 8.4 Performance Testing
- [ ] Create load testing scenarios
- [ ] Test search performance (< 500ms target)
- [ ] Validate public record access (< 300ms target)
- [ ] Test concurrent user scenarios
- [ ] Build performance regression tests

## Phase 9: Deployment & DevOps (Weeks 31-32)

### 9.1 CI/CD Pipeline
- [ ] Set up GitHub Actions workflows
- [ ] Configure automated testing in pipeline
- [ ] Build Docker image automation
- [ ] Set up ECR push automation
- [ ] Create EKS deployment automation

### 9.2 Infrastructure as Code
- [ ] Create Terraform/CloudFormation templates
- [ ] Set up environment provisioning
- [ ] Configure secrets management
- [ ] Build disaster recovery procedures
- [ ] Create backup and restore processes

### 9.3 Production Deployment
- [ ] Set up production environment
- [ ] Configure auto-scaling (HPA)
- [ ] Implement blue-green deployment
- [ ] Set up database migrations
- [ ] Configure monitoring in production

### 9.4 Security Hardening
- [ ] Conduct security audit
- [ ] Implement penetration testing
- [ ] Configure WAF and security groups
- [ ] Set up vulnerability scanning
- [ ] Create security incident response plan

## Phase 10: Launch Preparation (Weeks 33-36)

### 10.1 User Acceptance Testing
- [ ] Conduct stakeholder testing
- [ ] Perform compliance validation
- [ ] Test with Companies Office staff
- [ ] Validate public user workflows
- [ ] Create user feedback collection

### 10.2 Documentation
- [ ] Create developer documentation
- [ ] Build API documentation
- [ ] Write user guides
- [ ] Create admin manuals
- [ ] Build troubleshooting guides

### 10.3 Training & Support
- [ ] Train Companies Office staff
- [ ] Create support procedures
- [ ] Build help desk integration
- [ ] Create user training materials
- [ ] Set up feedback channels

### 10.4 Go-Live Preparation
- [ ] Conduct final security review
- [ ] Perform load testing at scale
- [ ] Create rollback procedures
- [ ] Set up production monitoring
- [ ] Plan launch communications

## Post-Launch (Ongoing)

### Maintenance & Support
- [ ] Monitor system performance
- [ ] Address user feedback
- [ ] Apply security updates
- [ ] Optimize performance
- [ ] Plan feature enhancements

### Future Enhancements
- [ ] Implement bilingual support (English/Mori)
- [ ] Add mobile application
- [ ] Enhance search capabilities
- [ ] Integrate additional government services
- [ ] Expand API capabilities

---

## Key Milestones

| Phase | Milestone | Target Week |
|-------|-----------|-------------|
| 1 | Foundation Complete | Week 4 |
| 2 | Core APIs Ready | Week 8 |
| 3 | Registration Flow Live | Week 12 |
| 4 | Compliance System Active | Week 16 |
| 5 | Public Search Available | Week 20 |
| 6 | Advanced Features Complete | Week 24 |
| 7 | Monitoring Operational | Week 26 |
| 8 | Testing Complete | Week 30 |
| 9 | Production Ready | Week 32 |
| 10 | System Launch | Week 36 |

## Success Criteria

- [ ] 99.9% uptime SLA achieved
- [ ] Search queries < 500ms response time
- [ ] Public record views < 300ms median
- [ ] 90%+ unit test coverage
- [ ] 80%+ integration test coverage
- [ ] WCAG 2.1 AA compliance
- [ ] Companies Act 1993 full compliance
- [ ] Successful UAT with stakeholders