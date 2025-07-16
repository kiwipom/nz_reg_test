from aws_cdk import (
    Stack,
    aws_ec2 as ec2,
    Tags,
)
from constructs import Construct


class VpcStack(Stack):
    def __init__(self, scope: Construct, construct_id: str, **kwargs) -> None:
        super().__init__(scope, construct_id, **kwargs)

        # VPC with public and private subnets across 3 AZs
        self.vpc = ec2.Vpc(
            self,
            "NzCompaniesVpc",
            vpc_name="nz-companies-register-vpc",
            max_azs=3,
            subnet_configuration=[
                ec2.SubnetConfiguration(
                    name="Public",
                    subnet_type=ec2.SubnetType.PUBLIC,
                    cidr_mask=24,
                ),
                ec2.SubnetConfiguration(
                    name="Private",
                    subnet_type=ec2.SubnetType.PRIVATE_WITH_EGRESS,
                    cidr_mask=24,
                ),
                ec2.SubnetConfiguration(
                    name="Database",
                    subnet_type=ec2.SubnetType.PRIVATE_ISOLATED,
                    cidr_mask=24,
                ),
            ],
            enable_dns_hostnames=True,
            enable_dns_support=True,
        )

        # VPC Flow Logs for security monitoring
        self.vpc.add_flow_log(
            "FlowLogCloudWatch",
            destination=ec2.FlowLogDestination.to_cloud_watch_logs(),
            traffic_type=ec2.FlowLogTrafficType.ALL,
        )

        # Security Groups
        self.alb_security_group = ec2.SecurityGroup(
            self,
            "AlbSecurityGroup",
            vpc=self.vpc,
            description="Security group for Application Load Balancer",
            security_group_name="nz-companies-alb-sg",
        )
        self.alb_security_group.add_ingress_rule(
            ec2.Peer.any_ipv4(), ec2.Port.tcp(80), "Allow HTTP from internet"
        )
        self.alb_security_group.add_ingress_rule(
            ec2.Peer.any_ipv4(), ec2.Port.tcp(443), "Allow HTTPS from internet"
        )

        self.ecs_security_group = ec2.SecurityGroup(
            self,
            "EcsSecurityGroup",
            vpc=self.vpc,
            description="Security group for ECS tasks",
            security_group_name="nz-companies-ecs-sg",
        )
        self.ecs_security_group.add_ingress_rule(
            self.alb_security_group,
            ec2.Port.tcp(8080),
            "Allow traffic from ALB",
        )

        self.database_security_group = ec2.SecurityGroup(
            self,
            "DatabaseSecurityGroup",
            vpc=self.vpc,
            description="Security group for RDS database",
            security_group_name="nz-companies-db-sg",
        )
        self.database_security_group.add_ingress_rule(
            self.ecs_security_group,
            ec2.Port.tcp(5432),
            "Allow PostgreSQL from ECS",
        )

        # Add tags
        Tags.of(self).add("Project", "NZ Companies Register")
        Tags.of(self).add("Environment", "Production")
        Tags.of(self).add("Component", "Networking")
