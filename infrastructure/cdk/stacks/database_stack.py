from aws_cdk import (
    Stack,
    aws_rds as rds,
    aws_ec2 as ec2,
    aws_secretsmanager as secretsmanager,
    RemovalPolicy,
    Tags,
    Duration,
)
from constructs import Construct


class DatabaseStack(Stack):
    def __init__(
        self, scope: Construct, construct_id: str, vpc: ec2.Vpc, **kwargs
    ) -> None:
        super().__init__(scope, construct_id, **kwargs)

        # Database credentials secret
        self.database_secret = secretsmanager.Secret(
            self,
            "DatabaseSecret",
            secret_name="nz-companies-register/database",
            description="Database credentials for NZ Companies Register",
            generate_secret_string=secretsmanager.SecretStringGenerator(
                secret_string_template='{"username": "postgres"}',
                generate_string_key="password",
                exclude_characters=" \"\\/@'",
                password_length=32,
            ),
        )

        # Database subnet group
        db_subnet_group = rds.SubnetGroup(
            self,
            "DatabaseSubnetGroup",
            description="Subnet group for NZ Companies Register database",
            vpc=vpc,
            subnet_group_name="nz-companies-db-subnet-group",
            vpc_subnets=ec2.SubnetSelection(
                subnet_type=ec2.SubnetType.PRIVATE_ISOLATED
            ),
        )

        # Database parameter group
        db_parameter_group = rds.ParameterGroup(
            self,
            "DatabaseParameterGroup",
            engine=rds.DatabaseClusterEngine.aurora_postgres(
                version=rds.AuroraPostgresEngineVersion.VER_15_4
            ),
            description="Parameter group for NZ Companies Register database",
            parameters={
                "log_statement": "all",
                "log_min_duration_statement": "1000",
                "shared_preload_libraries": "pg_stat_statements",
                "max_connections": "200",
            },
        )

        # Aurora PostgreSQL cluster
        self.database = rds.DatabaseCluster(
            self,
            "Database",
            cluster_identifier="nz-companies-register-db",
            engine=rds.DatabaseClusterEngine.aurora_postgres(
                version=rds.AuroraPostgresEngineVersion.VER_15_4
            ),
            credentials=rds.Credentials.from_secret(self.database_secret),
            default_database_name="nz_companies_register",
            vpc=vpc,
            subnet_group=db_subnet_group,
            parameter_group=db_parameter_group,
            writer=rds.ClusterInstance.provisioned(
                "Writer",
                instance_type=ec2.InstanceType.of(
                    ec2.InstanceClass.R6G, ec2.InstanceSize.LARGE
                ),
                publicly_accessible=False,
                auto_minor_version_upgrade=True,
                allow_major_version_upgrade=False,
                enable_performance_insights=True,
                parameter_group=db_parameter_group,
            ),
            readers=[
                rds.ClusterInstance.provisioned(
                    "Reader1",
                    instance_type=ec2.InstanceType.of(
                        ec2.InstanceClass.R6G, ec2.InstanceSize.LARGE
                    ),
                    publicly_accessible=False,
                    auto_minor_version_upgrade=True,
                    allow_major_version_upgrade=False,
                    enable_performance_insights=True,
                    parameter_group=db_parameter_group,
                ),
            ],
            backup=rds.BackupProps(
                retention=Duration.days(30),
                preferred_window="03:00-04:00",
            ),
            preferred_maintenance_window="sun:04:00-sun:05:00",
            storage_encrypted=True,
            monitoring_interval=Duration.seconds(60),
            cloudwatch_logs_exports=["postgresql"],
            deletion_protection=True,
            removal_policy=RemovalPolicy.RETAIN,
        )

        # DynamoDB table for document metadata
        from aws_cdk import aws_dynamodb as dynamodb

        self.document_table = dynamodb.Table(
            self,
            "DocumentTable",
            table_name="nz-companies-register-documents",
            partition_key=dynamodb.Attribute(
                name="document_id", type=dynamodb.AttributeType.STRING
            ),
            sort_key=dynamodb.Attribute(
                name="version", type=dynamodb.AttributeType.STRING
            ),
            billing_mode=dynamodb.BillingMode.PAY_PER_REQUEST,
            encryption=dynamodb.TableEncryption.AWS_MANAGED,
            point_in_time_recovery=True,
            removal_policy=RemovalPolicy.RETAIN,
        )

        # Add GSI for querying by company_id
        self.document_table.add_global_secondary_index(
            index_name="CompanyIdIndex",
            partition_key=dynamodb.Attribute(
                name="company_id", type=dynamodb.AttributeType.STRING
            ),
            sort_key=dynamodb.Attribute(
                name="created_at", type=dynamodb.AttributeType.STRING
            ),
        )

        # Add tags
        Tags.of(self).add("Project", "NZ Companies Register")
        Tags.of(self).add("Environment", "Production")
        Tags.of(self).add("Component", "Database")
