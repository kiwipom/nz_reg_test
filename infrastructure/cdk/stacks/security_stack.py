from aws_cdk import (
    Stack,
    aws_iam as iam,
    aws_kms as kms,
    aws_secretsmanager as secretsmanager,
    aws_ssm as ssm,
    RemovalPolicy,
    Tags,
)
from constructs import Construct


class SecurityStack(Stack):
    def __init__(self, scope: Construct, construct_id: str, **kwargs) -> None:
        super().__init__(scope, construct_id, **kwargs)

        # KMS key for application-wide encryption
        self.application_key = kms.Key(
            self,
            "ApplicationKey",
            description=("KMS key for NZ Companies Register application encryption"),
            enable_key_rotation=True,
            removal_policy=RemovalPolicy.RETAIN,
        )

        # KMS alias for easier reference
        self.application_key_alias = kms.Alias(
            self,
            "ApplicationKeyAlias",
            alias_name="alias/nz-companies-register",
            target_key=self.application_key,
        )

        # IAM role for ECS tasks
        self.ecs_task_role = iam.Role(
            self,
            "EcsTaskRole",
            role_name="nz-companies-register-ecs-task-role",
            assumed_by=iam.ServicePrincipal("ecs-tasks.amazonaws.com"),
            description="Role for NZ Companies Register ECS tasks",
        )

        # IAM role for ECS task execution
        self.ecs_execution_role = iam.Role(
            self,
            "EcsExecutionRole",
            role_name="nz-companies-register-ecs-execution-role",
            assumed_by=iam.ServicePrincipal("ecs-tasks.amazonaws.com"),
            managed_policies=[
                iam.ManagedPolicy.from_aws_managed_policy_name(
                    "service-role/AmazonECSTaskExecutionRolePolicy"
                ),
            ],
            description="Role for ECS task execution",
        )

        # IAM policies for application services
        self.s3_policy = iam.PolicyDocument(
            statements=[
                iam.PolicyStatement(
                    effect=iam.Effect.ALLOW,
                    actions=[
                        "s3:GetObject",
                        "s3:PutObject",
                        "s3:DeleteObject",
                        "s3:ListBucket",
                    ],
                    resources=[
                        "arn:aws:s3:::nz-companies-register-documents",
                        "arn:aws:s3:::nz-companies-register-documents/*",
                    ],
                ),
            ]
        )

        self.dynamodb_policy = iam.PolicyDocument(
            statements=[
                iam.PolicyStatement(
                    effect=iam.Effect.ALLOW,
                    actions=[
                        "dynamodb:GetItem",
                        "dynamodb:PutItem",
                        "dynamodb:UpdateItem",
                        "dynamodb:DeleteItem",
                        "dynamodb:Query",
                        "dynamodb:Scan",
                    ],
                    resources=[
                        (
                            "arn:aws:dynamodb:*:*:table/"
                            "nz-companies-register-documents"
                        ),
                        (
                            "arn:aws:dynamodb:*:*:table/"
                            "nz-companies-register-documents/index/*"
                        ),
                    ],
                ),
            ]
        )

        self.sns_sqs_policy = iam.PolicyDocument(
            statements=[
                iam.PolicyStatement(
                    effect=iam.Effect.ALLOW,
                    actions=[
                        "sns:Publish",
                        "sqs:SendMessage",
                        "sqs:ReceiveMessage",
                        "sqs:DeleteMessage",
                        "sqs:GetQueueAttributes",
                    ],
                    resources=[
                        "arn:aws:sns:*:*:nz-companies-register-notifications",
                        "arn:aws:sqs:*:*:nz-companies-register-notifications",
                        "arn:aws:sqs:*:*:nz-companies-register-reminders",
                    ],
                ),
            ]
        )

        self.secrets_policy = iam.PolicyDocument(
            statements=[
                iam.PolicyStatement(
                    effect=iam.Effect.ALLOW,
                    actions=[
                        "secretsmanager:GetSecretValue",
                        "secretsmanager:DescribeSecret",
                    ],
                    resources=[
                        (
                            "arn:aws:secretsmanager:*:*:secret:"
                            "nz-companies-register/*"
                        ),
                    ],
                ),
            ]
        )

        # Attach policies to ECS task role
        iam.Policy(
            self,
            "S3Policy",
            policy_name="nz-companies-register-s3-policy",
            policy_document=self.s3_policy,
            roles=[self.ecs_task_role],
        )

        iam.Policy(
            self,
            "DynamoDBPolicy",
            policy_name="nz-companies-register-dynamodb-policy",
            policy_document=self.dynamodb_policy,
            roles=[self.ecs_task_role],
        )

        iam.Policy(
            self,
            "SNSSQSPolicy",
            policy_name="nz-companies-register-sns-sqs-policy",
            policy_document=self.sns_sqs_policy,
            roles=[self.ecs_task_role],
        )

        iam.Policy(
            self,
            "SecretsPolicy",
            policy_name="nz-companies-register-secrets-policy",
            policy_document=self.secrets_policy,
            roles=[self.ecs_task_role],
        )

        # Add KMS permissions to ECS task role
        self.application_key.grant_encrypt_decrypt(self.ecs_task_role)

        # Add CloudWatch permissions
        self.ecs_task_role.add_managed_policy(
            iam.ManagedPolicy.from_aws_managed_policy_name(
                "CloudWatchAgentServerPolicy"
            )
        )

        # Add X-Ray permissions for tracing
        self.ecs_task_role.add_managed_policy(
            iam.ManagedPolicy.from_aws_managed_policy_name("AWSXRayDaemonWriteAccess")
        )

        # ECS execution role additional permissions
        self.ecs_execution_role.add_to_policy(
            iam.PolicyStatement(
                effect=iam.Effect.ALLOW,
                actions=[
                    "secretsmanager:GetSecretValue",
                    "ssm:GetParameters",
                    "ssm:GetParameter",
                    "kms:Decrypt",
                ],
                resources=[
                    ("arn:aws:secretsmanager:*:*:secret:" "nz-companies-register/*"),
                    "arn:aws:ssm:*:*:parameter/nz-companies-register/*",
                    self.application_key.key_arn,
                ],
            )
        )

        # System parameters for configuration
        self.jwt_secret = secretsmanager.Secret(
            self,
            "JWTSecret",
            secret_name="nz-companies-register/jwt-secret",
            description="JWT signing secret for NZ Companies Register",
            generate_secret_string=secretsmanager.SecretStringGenerator(
                secret_string_template='{"jwt_secret": ""}',
                generate_string_key="jwt_secret",
                exclude_characters=" \"\\/@'",
                password_length=64,
            ),
        )

        # API keys for external services
        self.external_api_keys = secretsmanager.Secret(
            self,
            "ExternalAPIKeys",
            secret_name="nz-companies-register/external-api-keys",
            description="API keys for external services",
            generate_secret_string=secretsmanager.SecretStringGenerator(
                secret_string_template=(
                    '{"nz_post_api_key": "", "auth0_client_secret": ""}'
                ),
                generate_string_key="placeholder",
                exclude_characters=" \"\\/@'",
                password_length=32,
            ),
        )

        # SSM parameters for non-sensitive configuration
        ssm.StringParameter(
            self,
            "AppEnvironment",
            parameter_name="/nz-companies-register/environment",
            string_value="production",
            description="Application environment",
        )

        ssm.StringParameter(
            self,
            "AppVersion",
            parameter_name="/nz-companies-register/version",
            string_value="1.0.0",
            description="Application version",
        )

        ssm.StringParameter(
            self,
            "JWTIssuer",
            parameter_name="/nz-companies-register/jwt-issuer",
            string_value="https://nz-companies-register.auth0.com/",
            description="JWT issuer URL",
        )

        ssm.StringParameter(
            self,
            "CORSOrigins",
            parameter_name="/nz-companies-register/cors-origins",
            string_value=("https://companies.govt.nz,https://www.companies.govt.nz"),
            description="Allowed CORS origins",
        )

        # Add tags
        Tags.of(self).add("Project", "NZ Companies Register")
        Tags.of(self).add("Environment", "Production")
        Tags.of(self).add("Component", "Security")
