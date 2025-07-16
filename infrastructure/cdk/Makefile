.PHONY: install test lint format security check deploy diff synth clean help

# Default target
help:
	@echo "Available targets:"
	@echo "  install    - Install dependencies"
	@echo "  test       - Run tests"
	@echo "  lint       - Run linting"
	@echo "  format     - Format code"
	@echo "  security   - Run security checks"
	@echo "  check      - Run all checks (test, lint, security)"
	@echo "  synth      - Synthesize CDK app"
	@echo "  diff       - Show diff against deployed stack"
	@echo "  deploy     - Deploy stack"
	@echo "  clean      - Clean build artifacts"

install:
	pip install -r requirements.txt
	pip install -r test_requirements.txt

test:
	pytest tests/ -v --cov=stacks --cov-report=html --cov-report=term-missing

lint:
	flake8 stacks/ tests/
	mypy stacks/ tests/

format:
	black stacks/ tests/
	black --check stacks/ tests/

security:
	bandit -r stacks/

check: test lint security

synth:
	cdk synth

diff:
	cdk diff

deploy:
	cdk deploy --all --require-approval never

clean:
	rm -rf cdk.out/
	rm -rf .pytest_cache/
	rm -rf htmlcov/
	rm -rf .coverage
	find . -type d -name __pycache__ -exec rm -rf {} +
	find . -name "*.pyc" -delete

# Security-focused targets
security-test:
	pytest tests/ -m security

validate-templates:
	cdk synth --validation

# CI/CD helpers
ci-test: install test lint security

ci-deploy: install synth deploy