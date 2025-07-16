import pytest
from aws_cdk import App
from aws_cdk.assertions import Template, Match
from stacks.vpc_stack import VpcStack
from stacks.database_stack import DatabaseStack


class TestDatabaseStack:
    @pytest.fixture
    def template(self):
        app = App()
        vpc_stack = VpcStack(app, "TestVpcStack")
        database_stack = DatabaseStack(app, "TestDatabaseStack", vpc=vpc_stack.vpc)
        return Template.from_stack(database_stack)

    def test_database_secret_created(self, template):
        """Test that database secret is created"""
        template.has_resource_properties("AWS::SecretsManager::Secret", {
            "Name": "nz-companies-register/database",
            "Description": "Database credentials for NZ Companies Register",
        })

    def test_aurora_cluster_created(self, template):
        """Test that Aurora PostgreSQL cluster is created"""
        template.has_resource_properties("AWS::RDS::DBCluster", {
            "Engine": "aurora-postgresql",
            "DatabaseName": "nz_companies_register",
            "StorageEncrypted": True,
            "DeletionProtection": True,
            "BackupRetentionPeriod": 30,
        })

    def test_database_instances_created(self, template):
        """Test that database instances are created"""
        # Writer instance
        template.has_resource_properties("AWS::RDS::DBInstance", {
            "DBInstanceClass": "db.r6g.large",
            "PubliclyAccessible": False,
            "AutoMinorVersionUpgrade": True,
            "AllowMajorVersionUpgrade": False,
            "DeleteAutomatedBackups": False,
            "MonitoringInterval": 60,
            "PerformanceInsightsEnabled": True,
        })

    def test_database_subnet_group_created(self, template):
        """Test that database subnet group is created"""
        template.has_resource_properties("AWS::RDS::DBSubnetGroup", {
            "DBSubnetGroupDescription": "Subnet group for NZ Companies Register database",
            "DBSubnetGroupName": "nz-companies-db-subnet-group",
        })

    def test_database_parameter_group_created(self, template):
        """Test that database parameter group is created"""
        template.has_resource_properties("AWS::RDS::DBParameterGroup", {
            "Description": "Parameter group for NZ Companies Register database",
            "Parameters": {
                "log_statement": "all",
                "log_min_duration_statement": "1000",
                "shared_preload_libraries": "pg_stat_statements",
                "max_connections": "200",
            },
        })

    def test_dynamodb_table_created(self, template):
        """Test that DynamoDB table is created"""
        template.has_resource_properties("AWS::DynamoDB::Table", {
            "TableName": "nz-companies-register-documents",
            "AttributeDefinitions": [
                {"AttributeName": "document_id", "AttributeType": "S"},
                {"AttributeName": "version", "AttributeType": "S"},
                {"AttributeName": "company_id", "AttributeType": "S"},
                {"AttributeName": "created_at", "AttributeType": "S"},
            ],
            "KeySchema": [
                {"AttributeName": "document_id", "KeyType": "HASH"},
                {"AttributeName": "version", "KeyType": "RANGE"},
            ],
            "BillingMode": "PAY_PER_REQUEST",
            "SSESpecification": {
                "SSEEnabled": True,
            },
            "PointInTimeRecoverySpecification": {
                "PointInTimeRecoveryEnabled": True,
            },
        })

    def test_dynamodb_gsi_created(self, template):
        """Test that DynamoDB GSI is created"""
        template.has_resource_properties("AWS::DynamoDB::Table", {
            "GlobalSecondaryIndexes": [
                {
                    "IndexName": "CompanyIdIndex",
                    "KeySchema": [
                        {"AttributeName": "company_id", "KeyType": "HASH"},
                        {"AttributeName": "created_at", "KeyType": "RANGE"},
                    ],
                }
            ],
        })

    def test_cloudwatch_log_groups_created(self, template):
        """Test that CloudWatch log groups are created for RDS"""
        template.has_resource_properties("AWS::RDS::DBCluster", {
            "EnableCloudwatchLogsExports": ["postgresql"],
        })

    def test_backup_configuration(self, template):
        """Test that backup is properly configured"""
        template.has_resource_properties("AWS::RDS::DBCluster", {
            "BackupRetentionPeriod": 30,
            "PreferredBackupWindow": "03:00-04:00",
            "PreferredMaintenanceWindow": "sun:04:00-sun:05:00",
        })

    def test_security_configuration(self, template):
        """Test that security is properly configured"""
        template.has_resource_properties("AWS::RDS::DBCluster", {
            "StorageEncrypted": True,
            "DeletionProtection": True,
        })

    def test_tags_applied(self, template):
        """Test that proper tags are applied"""
        template.has_resource_properties("AWS::RDS::DBCluster", {
            "Tags": Match.array_with([
                {"Key": "Project", "Value": "NZ Companies Register"},
                {"Key": "Environment", "Value": "Production"},
                {"Key": "Component", "Value": "Database"},
            ])
        })