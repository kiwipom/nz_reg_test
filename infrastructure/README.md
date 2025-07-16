# Infrastructure - NZ Companies Register

AWS CDK infrastructure as code for the NZ Companies Register system, implementing a secure, scalable, and compliant cloud architecture.

## 🏗️ Architecture

- **IaC Framework**: AWS CDK with Python
- **Cloud Provider**: AWS (ap-southeast-2)
- **Deployment**: Multi-AZ, auto-scaling
- **Security**: KMS encryption, IAM roles, VPC
- **Monitoring**: CloudWatch, Prometheus, Grafana
- **Testing**: pytest with comprehensive coverage

## 🚀 Quick Start

### Prerequisites
- Python 3.9+
- AWS CLI configured
- Node.js 18+ (for CDK CLI)

### Setup

1. **Install CDK CLI**:
   ```bash
   npm install -g aws-cdk
   ```

2. **Install dependencies**:
   ```bash
   cd infrastructure/cdk
   pip install -r requirements.txt
   pip install -r test_requirements.txt
   ```

3. **Bootstrap CDK** (first time only):
   ```bash
   cdk bootstrap
   ```

### Development Workflow

```bash
# Run tests
make test

# Lint code
make lint

# Format code
make format

# Security scan
make security

# Synthesize CloudFormation
make synth

# Deploy (when ready)
make deploy
```

## 📁 Project Structure

```
infrastructure/
├── cdk/
│   ├── stacks/              # CDK stack definitions
│   │   ├── vpc_stack.py     # VPC and networking
│   │   ├── security_stack.py # IAM roles and KMS
│   │   ├── database_stack.py # RDS and DynamoDB
│   │   ├── storage_stack.py  # S3 and messaging
│   │   ├── compute_stack.py  # EKS and containers
│   │   └── monitoring_stack.py # CloudWatch and alerting
│   ├── tests/               # Infrastructure tests
│   │   ├── test_vpc_stack.py
│   │   ├── test_database_stack.py
│   │   └── test_storage_stack.py
│   ├── app.py               # CDK app entry point
│   ├── cdk.json             # CDK configuration
│   ├── requirements.txt     # Python dependencies
│   ├── test_requirements.txt # Test dependencies
│   ├── pytest.ini           # Test configuration
│   └── Makefile            # Build automation
└── README.md               # This file
```

## 🧪 Testing

### Run Tests
```bash
# Run all tests with coverage
make test

# Run specific test file
pytest tests/test_vpc_stack.py -v

# Run with verbose output
pytest tests/ -v --tb=short

# Run security-focused tests
pytest tests/ -m security
```

### Test Coverage
- **Target**: 90%+ line coverage
- **Reports**: `htmlcov/index.html`
- **Framework**: pytest with pytest-cov

### Test Types
- **Unit Tests**: Stack resource validation
- **Integration Tests**: Cross-stack dependencies
- **Security Tests**: Compliance and security checks
- **Smoke Tests**: Basic deployment validation

## 🔧 Development

### Code Quality
```bash
# Format code
make format

# Check formatting
black --check stacks/ tests/

# Lint code
make lint

# Type checking
mypy stacks/ tests/

# Security scan
make security
```

### CDK Commands
```bash
# List all stacks
cdk ls

# Show differences
cdk diff

# Synthesize CloudFormation
cdk synth

# Deploy specific stack
cdk deploy NzCompaniesVpc

# Deploy all stacks
cdk deploy --all

# Destroy stack
cdk destroy NzCompaniesVpc
```

## 🏛️ Stack Architecture

### 1. VPC Stack (`vpc_stack.py`)
- **Multi-AZ VPC** with public, private, and database subnets
- **Security Groups** with least-privilege access
- **NAT Gateways** for outbound internet access
- **VPC Flow Logs** for network monitoring

### 2. Security Stack (`security_stack.py`)
- **IAM Roles** for ECS tasks and services
- **KMS Keys** for encryption at rest
- **Secrets Manager** for sensitive data
- **SSM Parameters** for configuration

### 3. Database Stack (`database_stack.py`)
- **RDS Aurora PostgreSQL** with Multi-AZ deployment
- **Read Replicas** for scalability
- **DynamoDB Tables** for document metadata
- **Automated Backups** with point-in-time recovery

### 4. Storage Stack (`storage_stack.py`)
- **S3 Buckets** with lifecycle policies
- **SNS Topics** for notifications
- **SQS Queues** with dead letter queues
- **KMS Encryption** for all storage

### 5. Compute Stack (`compute_stack.py`)
- **EKS Cluster** with Fargate
- **ECR Repositories** for container images
- **Application Load Balancer** with SSL termination
- **Auto Scaling Groups** with health checks

### 6. Monitoring Stack (`monitoring_stack.py`)
- **CloudWatch Alarms** and dashboards
- **Custom Metrics** for business KPIs
- **Log Groups** with retention policies
- **SNS Alerts** for operational notifications

## 🔐 Security

### Security Features
- **VPC Isolation**: Private subnets with no direct internet access
- **Encryption**: KMS encryption for all data at rest
- **IAM Policies**: Least privilege access controls
- **Network ACLs**: Additional layer of network security
- **Security Groups**: Application-level firewall rules

### Compliance
- **Companies Act 1993**: Full compliance implementation
- **Privacy Act 2020**: Data protection measures
- **ISO 27001**: Security management standards
- **SOC 2 Type II**: Operational security controls

### Security Testing
```bash
# Run security-focused tests
pytest tests/ -m security

# Security scan with bandit
bandit -r stacks/

# Check for known vulnerabilities
safety check
```

## 📊 Monitoring

### CloudWatch Integration
- **Application Metrics**: Response times, error rates
- **Infrastructure Metrics**: CPU, memory, disk usage
- **Custom Metrics**: Business KPIs and compliance
- **Log Aggregation**: Centralized logging

### Alerting
- **High Severity**: Database failures, security breaches
- **Medium Severity**: Performance degradation
- **Low Severity**: Informational alerts
- **Escalation**: PagerDuty integration

### Dashboards
- **Executive Dashboard**: Business metrics
- **Operations Dashboard**: System health
- **Security Dashboard**: Security events
- **Compliance Dashboard**: Regulatory metrics

## 🚀 Deployment

### Environment Configuration
```bash
# Set AWS region
export AWS_DEFAULT_REGION=ap-southeast-2

# Set environment context
cdk deploy --context environment=production

# Use specific AWS profile
aws configure --profile nz-companies
export AWS_PROFILE=nz-companies
```

### Deployment Strategy
1. **VPC Stack**: Network foundation
2. **Security Stack**: IAM roles and KMS keys
3. **Database Stack**: Data layer
4. **Storage Stack**: File storage and messaging
5. **Compute Stack**: Application containers
6. **Monitoring Stack**: Observability

### Production Deployment
```bash
# Deploy to production
make ci-deploy

# Rollback if needed
cdk destroy --force

# Health check
curl https://api.companies.govt.nz/health
```

## 💰 Cost Optimization

### Cost Management
- **Right-sizing**: Appropriate instance types
- **Reserved Instances**: Cost savings for predictable workloads
- **Spot Instances**: Cost-effective for batch processing
- **Auto Scaling**: Scale down during low usage

### Cost Monitoring
```bash
# Check estimated costs
aws ce get-cost-and-usage --time-period Start=2024-01-01,End=2024-01-31

# Set up cost alerts
aws budgets create-budget --account-id 123456789012 --budget file://budget.json
```

## 🔧 Configuration

### CDK Configuration (`cdk.json`)
```json
{
  "app": "python3 app.py",
  "context": {
    "account": "123456789012",
    "region": "ap-southeast-2",
    "@aws-cdk/core:enableStackNameDuplicates": false,
    "@aws-cdk/core:stackRelativeExports": true
  }
}
```

### Environment Variables
```bash
# AWS Configuration
export AWS_ACCOUNT_ID=123456789012
export AWS_DEFAULT_REGION=ap-southeast-2

# CDK Configuration
export CDK_DEFAULT_ACCOUNT=123456789012
export CDK_DEFAULT_REGION=ap-southeast-2

# Application Configuration
export ENVIRONMENT=production
export LOG_LEVEL=INFO
```

## 🚨 Troubleshooting

### Common Issues

1. **CDK Bootstrap Error**:
   ```bash
   # Re-bootstrap with correct permissions
   cdk bootstrap --trust 123456789012
   ```

2. **Stack Dependency Issues**:
   ```bash
   # Check stack dependencies
   cdk ls --long
   
   # Deploy in correct order
   cdk deploy NzCompaniesVpc NzCompaniesSecurity
   ```

3. **Resource Limit Errors**:
   ```bash
   # Check AWS service limits
   aws service-quotas get-service-quota --service-code ec2 --quota-code L-1216C47A
   
   # Request limit increase
   aws service-quotas request-service-quota-increase --service-code ec2 --quota-code L-1216C47A --desired-value 20
   ```

4. **Permission Errors**:
   ```bash
   # Check IAM permissions
   aws iam get-role --role-name CDKToolkit-cfn-exec-role
   
   # Verify CDK execution role
   aws sts get-caller-identity
   ```

### Debug Commands
```bash
# Enable CDK debug logging
export CDK_DEBUG=true

# Verbose CDK output
cdk synth --verbose

# Check CloudFormation events
aws cloudformation describe-stack-events --stack-name NzCompaniesVpc
```

## 📚 Dependencies

### Core CDK Modules
- **aws-cdk-lib**: Core CDK library
- **constructs**: CDK constructs
- **aws-cdk.aws-lambda-python-alpha**: Python Lambda support

### AWS Service Modules
- **EC2**: VPC and networking
- **RDS**: Database services
- **S3**: Storage services
- **EKS**: Kubernetes service
- **IAM**: Identity and access management

### Development Tools
- **pytest**: Testing framework
- **black**: Code formatting
- **mypy**: Type checking
- **bandit**: Security scanning

## 🤝 Contributing

### Development Workflow
1. Create feature branch
2. Make infrastructure changes
3. Add/update tests
4. Run `make check` (test + lint + security)
5. Submit pull request

### Code Standards
- **Python**: PEP 8 with Black formatting
- **CDK**: Follow CDK best practices
- **Security**: Security-first approach
- **Testing**: Test-driven development

### Pre-commit Checklist
- [ ] Tests pass: `make test`
- [ ] Linting passes: `make lint`
- [ ] Security scan clean: `make security`
- [ ] CDK synth succeeds: `make synth`
- [ ] Documentation updated

## 📖 Documentation

### CDK Resources
- **AWS CDK Documentation**: https://docs.aws.amazon.com/cdk/
- **CDK Python API Reference**: https://docs.aws.amazon.com/cdk/api/latest/python/
- **CDK Patterns**: https://cdkpatterns.com/

### Architecture Decisions
- **ADRs**: Architecture Decision Records in `docs/adr/`
- **Security**: Security architecture documentation
- **Compliance**: Regulatory compliance documentation

---

**Need help?** Check the [main README](../README.md) or create an issue.