import pytest
from aws_cdk import App
from aws_cdk.assertions import Template, Match
from stacks.vpc_stack import VpcStack


class TestVpcStack:
    @pytest.fixture
    def template(self):
        app = App()
        stack = VpcStack(app, "TestVpcStack")
        return Template.from_stack(stack)

    def test_vpc_created(self, template):
        """Test that VPC is created with correct configuration"""
        template.has_resource_properties(
            "AWS::EC2::VPC",
            {
                "CidrBlock": "10.0.0.0/16",
                "EnableDnsHostnames": True,
                "EnableDnsSupport": True,
            },
        )

    def test_subnets_created(self, template):
        """Test that public, private, and database subnets are created"""
        # Public subnets
        template.has_resource_properties(
            "AWS::EC2::Subnet",
            {
                "MapPublicIpOnLaunch": True,
            },
        )

        # Private subnets
        template.has_resource_properties(
            "AWS::EC2::Subnet",
            {
                "MapPublicIpOnLaunch": False,
            },
        )

    def test_internet_gateway_created(self, template):
        """Test that Internet Gateway is created"""
        template.has_resource("AWS::EC2::InternetGateway", {})

    def test_nat_gateways_created(self, template):
        """Test that NAT Gateways are created for private subnets"""
        template.has_resource("AWS::EC2::NatGateway", {})

    def test_security_groups_created(self, template):
        """Test that required security groups are created"""
        # ALB Security Group
        template.has_resource_properties(
            "AWS::EC2::SecurityGroup",
            {
                "GroupDescription": "Security group for Application Load Balancer",
                "SecurityGroupIngress": [
                    {
                        "IpProtocol": "tcp",
                        "FromPort": 80,
                        "ToPort": 80,
                        "CidrIp": "0.0.0.0/0",
                    },
                    {
                        "IpProtocol": "tcp",
                        "FromPort": 443,
                        "ToPort": 443,
                        "CidrIp": "0.0.0.0/0",
                    },
                ],
            },
        )

        # ECS Security Group
        template.has_resource_properties(
            "AWS::EC2::SecurityGroup",
            {
                "GroupDescription": "Security group for ECS tasks",
            },
        )

        # Database Security Group
        template.has_resource_properties(
            "AWS::EC2::SecurityGroup",
            {
                "GroupDescription": "Security group for RDS database",
            },
        )

    def test_flow_logs_enabled(self, template):
        """Test that VPC Flow Logs are enabled"""
        template.has_resource_properties(
            "AWS::EC2::FlowLog",
            {
                "ResourceType": "VPC",
                "TrafficType": "ALL",
            },
        )

    def test_tags_applied(self, template):
        """Test that proper tags are applied"""
        # Test each tag individually to be more resilient to tag ordering
        template.has_resource_properties(
            "AWS::EC2::VPC",
            {
                "Tags": Match.array_with(
                    [{"Key": "Project", "Value": "NZ Companies Register"}]
                )
            },
        )
        template.has_resource_properties(
            "AWS::EC2::VPC",
            {"Tags": Match.array_with([{"Key": "Environment", "Value": "Production"}])},
        )
        template.has_resource_properties(
            "AWS::EC2::VPC",
            {"Tags": Match.array_with([{"Key": "Component", "Value": "Networking"}])},
        )

    def test_availability_zones(self, template):
        """Test that resources are distributed across multiple AZs"""
        # Should have subnets in multiple AZs
        subnet_count = template.find_resources("AWS::EC2::Subnet")
        assert len(subnet_count) >= 6  # 3 AZs * 2 subnet types minimum

    def test_route_tables_configured(self, template):
        """Test that route tables are properly configured"""
        template.has_resource("AWS::EC2::RouteTable", {})
        template.has_resource("AWS::EC2::Route", {})
