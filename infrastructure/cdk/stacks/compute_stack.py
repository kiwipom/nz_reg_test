from aws_cdk import (
    Stack,
    aws_ec2 as ec2,
    aws_ecs as ecs,
    aws_ecr as ecr,
    aws_elbv2 as elbv2,
    aws_logs as logs,
    aws_iam as iam,
    aws_rds as rds,
    aws_s3 as s3,
    Duration,
    RemovalPolicy,
    Tags,
)
from constructs import Construct


class ComputeStack(Stack):
    def __init__(
        self,
        scope: Construct,
        construct_id: str,
        vpc: ec2.Vpc,
        database: rds.DatabaseCluster,
        document_bucket: s3.Bucket,
        **kwargs
    ) -> None:
        super().__init__(scope, construct_id, **kwargs)

        # ECR repositories
        self.backend_repository = ecr.Repository(
            self,
            "BackendRepository",
            repository_name="nz-companies-register/backend",
            image_scan_on_push=True,
            lifecycle_rules=[
                ecr.LifecycleRule(
                    description="Keep last 10 images",
                    max_image_count=10,
                ),
            ],
        )

        self.frontend_repository = ecr.Repository(
            self,
            "FrontendRepository",
            repository_name="nz-companies-register/frontend",
            image_scan_on_push=True,
            lifecycle_rules=[
                ecr.LifecycleRule(
                    description="Keep last 10 images",
                    max_image_count=10,
                ),
            ],
        )

        # ECS cluster
        self.cluster = ecs.Cluster(
            self,
            "Cluster",
            cluster_name="nz-companies-register-cluster",
            vpc=vpc,
            enable_fargate_capacity_providers=True,
            container_insights=True,
        )

        # Application Load Balancer
        self.alb = elbv2.ApplicationLoadBalancer(
            self,
            "ALB",
            load_balancer_name="nz-companies-register-alb",
            vpc=vpc,
            internet_facing=True,
            # Will be replaced with proper SG
            security_group=vpc.vpc_default_security_group,
        )

        # Target groups
        self.backend_target_group = elbv2.ApplicationTargetGroup(
            self,
            "BackendTargetGroup",
            target_group_name="nz-companies-backend-tg",
            port=8080,
            protocol=elbv2.ApplicationProtocol.HTTP,
            vpc=vpc,
            target_type=elbv2.TargetType.IP,
            health_check=elbv2.HealthCheck(
                enabled=True,
                path="/api/actuator/health",
                interval=Duration.seconds(30),
                timeout=Duration.seconds(5),
                healthy_threshold_count=2,
                unhealthy_threshold_count=5,
            ),
        )

        self.frontend_target_group = elbv2.ApplicationTargetGroup(
            self,
            "FrontendTargetGroup",
            target_group_name="nz-companies-frontend-tg",
            port=80,
            protocol=elbv2.ApplicationProtocol.HTTP,
            vpc=vpc,
            target_type=elbv2.TargetType.IP,
            health_check=elbv2.HealthCheck(
                enabled=True,
                path="/",
                interval=Duration.seconds(30),
                timeout=Duration.seconds(5),
                healthy_threshold_count=2,
                unhealthy_threshold_count=5,
            ),
        )

        # ALB listeners
        self.http_listener = self.alb.add_listener(
            "HTTPListener",
            port=80,
            protocol=elbv2.ApplicationProtocol.HTTP,
            default_action=elbv2.ListenerAction.redirect(
                protocol="HTTPS",
                port="443",
                permanent=True,
            ),
        )

        self.https_listener = self.alb.add_listener(
            "HTTPSListener",
            port=443,
            protocol=elbv2.ApplicationProtocol.HTTPS,
            default_action=elbv2.ListenerAction.fixed_response(
                status_code=404,
                content_type="text/plain",
                message_body="Not Found",
            ),
        )

        # Listener rules
        self.https_listener.add_action(
            "BackendRule",
            priority=100,
            conditions=[
                elbv2.ListenerCondition.path_patterns(["/api/*"]),
            ],
            action=elbv2.ListenerAction.forward([self.backend_target_group]),
        )

        self.https_listener.add_action(
            "FrontendRule",
            priority=200,
            conditions=[
                elbv2.ListenerCondition.path_patterns(["/*"]),
            ],
            action=elbv2.ListenerAction.forward([self.frontend_target_group]),
        )

        # CloudWatch Log Groups
        self.backend_log_group = logs.LogGroup(
            self,
            "BackendLogGroup",
            log_group_name="/ecs/nz-companies-register-backend",
            retention=logs.RetentionDays.ONE_MONTH,
            removal_policy=RemovalPolicy.DESTROY,
        )

        self.frontend_log_group = logs.LogGroup(
            self,
            "FrontendLogGroup",
            log_group_name="/ecs/nz-companies-register-frontend",
            retention=logs.RetentionDays.ONE_MONTH,
            removal_policy=RemovalPolicy.DESTROY,
        )

        # ECS Task Definitions
        self.backend_task_definition = ecs.FargateTaskDefinition(
            self,
            "BackendTaskDefinition",
            family="nz-companies-register-backend",
            memory_limit_mib=2048,
            cpu=1024,
            execution_role=iam.Role.from_role_name(
                self,
                "BackendExecutionRole",
                "nz-companies-register-ecs-execution-role",
            ),
            task_role=iam.Role.from_role_name(
                self,
                "BackendTaskRole",
                "nz-companies-register-ecs-task-role",
            ),
        )

        self.frontend_task_definition = ecs.FargateTaskDefinition(
            self,
            "FrontendTaskDefinition",
            family="nz-companies-register-frontend",
            memory_limit_mib=512,
            cpu=256,
            execution_role=iam.Role.from_role_name(
                self,
                "FrontendExecutionRole",
                "nz-companies-register-ecs-execution-role",
            ),
        )

        # Container definitions
        self.backend_container = self.backend_task_definition.add_container(
            "BackendContainer",
            image=ecs.ContainerImage.from_ecr_repository(
                self.backend_repository, "latest"
            ),
            container_name="nz-companies-register-backend",
            environment={
                "SPRING_PROFILES_ACTIVE": "production",
                "AWS_REGION": self.region,
                "DB_HOST": database.cluster_endpoint.hostname,
                "DB_PORT": "5432",
                "DB_NAME": "nz_companies_register",
                "S3_DOCUMENT_BUCKET": document_bucket.bucket_name,
            },
            secrets={
                "DB_USERNAME": ecs.Secret.from_secrets_manager(
                    database.secret, "username"
                ),
                "DB_PASSWORD": ecs.Secret.from_secrets_manager(
                    database.secret, "password"
                ),
            },
            logging=ecs.LogDrivers.aws_logs(
                stream_prefix="backend",
                log_group=self.backend_log_group,
            ),
            health_check=ecs.HealthCheck(
                command=[
                    "CMD-SHELL",
                    ("curl -f http://localhost:8080/api/actuator/health "
                     "|| exit 1"),
                ],
                interval=Duration.seconds(30),
                timeout=Duration.seconds(5),
                retries=3,
                start_period=Duration.seconds(60),
            ),
        )

        self.backend_container.add_port_mappings(
            ecs.PortMapping(
                container_port=8080,
                protocol=ecs.Protocol.TCP,
            )
        )

        self.frontend_container = self.frontend_task_definition.add_container(
            "FrontendContainer",
            image=ecs.ContainerImage.from_ecr_repository(
                self.frontend_repository, "latest"
            ),
            container_name="nz-companies-register-frontend",
            logging=ecs.LogDrivers.aws_logs(
                stream_prefix="frontend",
                log_group=self.frontend_log_group,
            ),
            health_check=ecs.HealthCheck(
                command=["CMD-SHELL", "curl -f http://localhost:80 || exit 1"],
                interval=Duration.seconds(30),
                timeout=Duration.seconds(5),
                retries=3,
                start_period=Duration.seconds(30),
            ),
        )

        self.frontend_container.add_port_mappings(
            ecs.PortMapping(
                container_port=80,
                protocol=ecs.Protocol.TCP,
            )
        )

        # ECS Services
        self.backend_service = ecs.FargateService(
            self,
            "BackendService",
            service_name="nz-companies-register-backend",
            cluster=self.cluster,
            task_definition=self.backend_task_definition,
            desired_count=2,
            assign_public_ip=False,
            vpc_subnets=ec2.SubnetSelection(
                subnet_type=ec2.SubnetType.PRIVATE_WITH_EGRESS
            ),
            enable_execute_command=True,
            enable_logging=True,
            circuit_breaker=ecs.DeploymentCircuitBreaker(
                rollback=True,
            ),
        )

        self.frontend_service = ecs.FargateService(
            self,
            "FrontendService",
            service_name="nz-companies-register-frontend",
            cluster=self.cluster,
            task_definition=self.frontend_task_definition,
            desired_count=2,
            assign_public_ip=False,
            vpc_subnets=ec2.SubnetSelection(
                subnet_type=ec2.SubnetType.PRIVATE_WITH_EGRESS
            ),
            enable_execute_command=True,
            enable_logging=True,
            circuit_breaker=ecs.DeploymentCircuitBreaker(
                rollback=True,
            ),
        )

        # Attach services to target groups
        self.backend_service.attach_to_application_target_group(
            self.backend_target_group
        )
        self.frontend_service.attach_to_application_target_group(
            self.frontend_target_group
        )

        # Auto Scaling
        self.backend_scaling = self.backend_service.auto_scale_task_count(
            min_capacity=2,
            max_capacity=10,
        )

        self.backend_scaling.scale_on_cpu_utilization(
            "BackendCPUScaling",
            target_utilization_percent=70,
            scale_in_cooldown=Duration.minutes(5),
            scale_out_cooldown=Duration.minutes(5),
        )

        self.backend_scaling.scale_on_memory_utilization(
            "BackendMemoryScaling",
            target_utilization_percent=80,
            scale_in_cooldown=Duration.minutes(5),
            scale_out_cooldown=Duration.minutes(5),
        )

        self.frontend_scaling = self.frontend_service.auto_scale_task_count(
            min_capacity=2,
            max_capacity=6,
        )

        self.frontend_scaling.scale_on_cpu_utilization(
            "FrontendCPUScaling",
            target_utilization_percent=70,
            scale_in_cooldown=Duration.minutes(5),
            scale_out_cooldown=Duration.minutes(5),
        )

        # Add tags
        Tags.of(self).add("Project", "NZ Companies Register")
        Tags.of(self).add("Environment", "Production")
        Tags.of(self).add("Component", "Compute")
