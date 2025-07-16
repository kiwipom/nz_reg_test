# NZ Companies Register System Specification

## 1. Purpose
To design and deliver a fully compliant, modern, modular software system that meets the obligations of the **Companies Act 1993**. The system will support the Companies Office in maintaining a national register of companies, capturing all statutory data, enforcing compliance obligations, surfacing public records, and supporting legal processes.

---

## 2. Functional Requirements

### 2.1 Data Capture
The system must provide forms and validations to collect the following data fields, as required under the Companies Act 1993:

#### 2.1.1 Company Registration
- Company Name (including name availability checks)
- Company Type (e.g., LTD, overseas company)
- Registered Office Address
- Address for Service
- Communication Address (email, phone)
- Incorporation Date
- Constitution (file upload, optional)
- Share Capital Details
  - Number of shares
  - Share types and rights
  - Allotment records
- Director Details
  - Full name
  - Date/place of birth
  - Residential address
  - Consent to act (digital attestation + record keeping)
- Shareholder Details
  - Full legal name
  - Address
  - Number and class of shares held
- Ultimate Holding Company (if applicable)

#### 2.1.2 Ongoing Updates
- Director appointments and resignations
- Changes to addresses
- Shareholder transfers
- Company constitution updates
- Name change applications
- Company amalgamations
- Liquidation, removal, or restoration requests

### 2.2 Workflow Management

#### 2.2.1 Annual Return Workflow
- Must send reminder notifications to companies:
  - 1 month before due date
  - 1 week before due date
  - On due date
  - Post-due follow-up (7 days, 30 days)
- Track status of return submissions
- Escalate to potential strike-off after 6+ months overdue

#### 2.2.2 Director Compliance
- Ensure each company has at least one NZ or Australian-resident director
- Ensure director consent is recorded
- Enforce disqualification rules

#### 2.2.3 Shareholding Snapshot
- Maintain immutable historical snapshots of shareholding and directorship at any point in time (per section 89 of the Act)
- Surface these via an API and user interface

#### 2.2.4 Public Register Access
- Searchable interface (by name, company number, director, shareholder)
- Public access to:
  - Current company record
  - Director list
  - Shareholders (where permitted)
  - Historical company extract at a given date
  - Filed documents

---

## 3. Technology Stack

### 3.1 Infrastructure
- **Hosting**: AWS (Aotearoa-local), multi-AZ, secure by default
- **Containerisation**: Docker + Kubernetes (EKS), single-tenant namespaces
- **Database**: Hybrid approach:
  - **Relational DB (PostgreSQL/Aurora)**: For transactional company records (core source of truth)
  - **Document DB (e.g., S3 + DynamoDB)**: For filings, documents, constitutional attachments, and snapshots
- **Storage**:
  - S3: Document archive, company extracts
  - EFS: Optional for shared microservice file handling

### 3.2 Backend Architecture
- Language: Kotlin (JVM), Spring Boot 3
- Architecture: Modular monolith with potential for microservices (e.g., notifications, snapshot service)
- Layers:
  - **API Layer**: REST + GraphQL, OAuth2/JWT Auth (via Auth0 or AWS Cognito)
  - **Service Layer**: Business logic with strict separation from API
  - **Persistence Layer**: Spring Data JPA + custom repositories where necessary
- Messaging:
  - SNS/SQS or Apache Kafka for inter-module communication
  - Use for event-driven features (reminders, logging, snapshot generation)

### 3.3 Front-End Architecture
- Framework: **React + TypeScript**
- Styling: **TailwindCSS**
- Component Library: **ShadCN/UI** or Radix UI
- Forms: Zod for validation, React Hook Form
- Routing: React Router
- State: Zustand or Redux Toolkit (for structured modules)

---

## 4. Non-Functional Requirements

### 4.1 Security
- Fully encrypted at rest and in transit
- Role-based access control (Admin, Public, Registrar, Internal Ops)
- Audit logs for all sensitive operations (company updates, removals, filings)

### 4.2 Observability
- **Application logs**: Structured JSON logs via FluentBit → CloudWatch or ELK
- **Infrastructure metrics**: Prometheus + Grafana dashboards
- **Tracing**: OpenTelemetry + Jaeger integration
- **Alerting**: CloudWatch alarms or Grafana Alerting integrated with PagerDuty

### 4.3 Performance & Reliability
- SLA: 99.9% uptime
- Auto-scaling containers via K8s HPA
- DB read replicas for scaling read-heavy workloads (search)
- Response time budget:
  - Search queries: < 500ms
  - Public record views: < 300ms median

### 4.4 Data Retention and Snapshots
- Maintain immutable, timestamped company record snapshots (daily or event-driven)
- Retain historic company extracts for minimum 7 years (or indefinitely per Registrar discretion)

### 4.5 Documentation and Support
- Developer documentation (OpenAPI spec, GraphQL playground, ADRs)
- User-facing help docs (Tech Writer to liaise with PM and BA)
- Support for manual overrides and data correction (e.g., registrar overrides)

---

## 5. Deliverables
- MVP scope defined by PM/BA including core registration, annual return, and search functionality
- Incremental releases, feature-flagged per environment
- CI/CD pipelines (GitHub Actions → ECR → EKS)
- Acceptance criteria defined per functional area
- Test coverage:
  - Unit: 90%+
  - Integration: 80%+
  - E2E: Defined and automated for public flows

---

## 6. Assumptions and Constraints
- All legal requirements sourced from Companies Act 1993 (as at 2025)
- System must comply with NZ privacy and data retention laws
- Public-facing APIs and UIs must be accessible and WCAG 2.1 AA compliant
- System must support bilingual (English/Māori) interfaces within 18 months of launch

---

## 7. Appendix

### 7.1 Key Decision Log

| Decision | Rationale | Trade-offs |
|----------|-----------|------------|
| Use of selective CQRS | Supports high-performance public search with optimised read models | Adds complexity, must manage eventual consistency |
| Kubernetes for orchestration | Enables runtime isolation, modular deployment, horizontal scaling | Adds operational overhead, requires platform maturity |
| Cloud-native services (AWS) | Better integration and performance, cost-effective | Reduces portability without adapters |
| Integration layer deferred | Start with direct integrations (e.g., NZ Post) and roadmap shared adapters | Simpler for MVP, less flexibility for future jurisdictions |
| API Gateway for public APIs | Handles throttling, authentication, metrics | Ties system closer to cloud platform (use standard interfaces where possible) |


### 7.2 Reference Material
- Companies Act 1993: https://www.legislation.govt.nz/act/public/1993/0105/latest/DLM319570.html
- NZBN Integration (future): for linking business identifiers
- Filing requirements defined in regulations and supported documents

