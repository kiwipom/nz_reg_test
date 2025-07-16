from aws_cdk import (
    Stack,
    aws_cloudwatch as cloudwatch,
    aws_logs as logs,
    aws_sns as sns,
    aws_cloudwatch_actions as cw_actions,
    aws_applicationautoscaling as appscaling,
    Duration,
    Tags,
)
from constructs import Construct
from .compute_stack import ComputeStack


class MonitoringStack(Stack):
    def __init__(
        self, scope: Construct, construct_id: str, compute_stack: ComputeStack, **kwargs
    ) -> None:
        super().__init__(scope, construct_id, **kwargs)

        # SNS topic for alerts
        self.alert_topic = sns.Topic(
            self,
            "AlertTopic",
            topic_name="nz-companies-register-alerts",
            display_name="NZ Companies Register Alerts",
        )

        # CloudWatch Dashboard
        self.dashboard = cloudwatch.Dashboard(
            self,
            "Dashboard",
            dashboard_name="NZ-Companies-Register-Dashboard",
        )

        # Application metrics
        self.create_application_alarms(compute_stack)
        self.create_infrastructure_alarms(compute_stack)
        self.create_database_alarms()
        self.create_custom_metrics()
        self.setup_dashboard_widgets(compute_stack)

    def create_application_alarms(self, compute_stack: ComputeStack):
        """Create application-level alarms"""
        # Backend service alarms
        cloudwatch.Alarm(
            self,
            "BackendHighCPU",
            alarm_name="nz-companies-register-backend-high-cpu",
            alarm_description="Backend service CPU utilization is high",
            metric=cloudwatch.Metric(
                namespace="AWS/ECS",
                metric_name="CPUUtilization",
                dimensions_map={
                    "ServiceName": compute_stack.backend_service.service_name,
                    "ClusterName": compute_stack.cluster.cluster_name,
                },
                statistic="Average",
                period=Duration.minutes(5),
            ),
            threshold=80,
            evaluation_periods=2,
            comparison_operator=cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            treat_missing_data=cloudwatch.TreatMissingData.NOT_BREACHING,
        ).add_alarm_action(cw_actions.SnsAction(self.alert_topic))

        cloudwatch.Alarm(
            self,
            "BackendHighMemory",
            alarm_name="nz-companies-register-backend-high-memory",
            alarm_description="Backend service memory utilization is high",
            metric=cloudwatch.Metric(
                namespace="AWS/ECS",
                metric_name="MemoryUtilization",
                dimensions_map={
                    "ServiceName": compute_stack.backend_service.service_name,
                    "ClusterName": compute_stack.cluster.cluster_name,
                },
                statistic="Average",
                period=Duration.minutes(5),
            ),
            threshold=85,
            evaluation_periods=2,
            comparison_operator=cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            treat_missing_data=cloudwatch.TreatMissingData.NOT_BREACHING,
        ).add_alarm_action(cw_actions.SnsAction(self.alert_topic))

        # Task count alarms
        cloudwatch.Alarm(
            self,
            "BackendTaskCountLow",
            alarm_name="nz-companies-register-backend-task-count-low",
            alarm_description="Backend service task count is below minimum",
            metric=cloudwatch.Metric(
                namespace="AWS/ECS",
                metric_name="RunningTaskCount",
                dimensions_map={
                    "ServiceName": compute_stack.backend_service.service_name,
                    "ClusterName": compute_stack.cluster.cluster_name,
                },
                statistic="Average",
                period=Duration.minutes(1),
            ),
            threshold=1,
            evaluation_periods=2,
            comparison_operator=cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
            treat_missing_data=cloudwatch.TreatMissingData.BREACHING,
        ).add_alarm_action(cw_actions.SnsAction(self.alert_topic))

    def create_infrastructure_alarms(self, compute_stack: ComputeStack):
        """Create infrastructure-level alarms"""
        # ALB alarms
        cloudwatch.Alarm(
            self,
            "ALBHighLatency",
            alarm_name="nz-companies-register-alb-high-latency",
            alarm_description="ALB target response time is high",
            metric=cloudwatch.Metric(
                namespace="AWS/ApplicationELB",
                metric_name="TargetResponseTime",
                dimensions_map={
                    "LoadBalancer": compute_stack.alb.load_balancer_full_name,
                },
                statistic="Average",
                period=Duration.minutes(5),
            ),
            threshold=1.0,  # 1 second
            evaluation_periods=2,
            comparison_operator=cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            treat_missing_data=cloudwatch.TreatMissingData.NOT_BREACHING,
        ).add_alarm_action(cw_actions.SnsAction(self.alert_topic))

        cloudwatch.Alarm(
            self,
            "ALBHighErrorRate",
            alarm_name="nz-companies-register-alb-high-error-rate",
            alarm_description="ALB 5xx error rate is high",
            metric=cloudwatch.Metric(
                namespace="AWS/ApplicationELB",
                metric_name="HTTPCode_Target_5XX_Count",
                dimensions_map={
                    "LoadBalancer": compute_stack.alb.load_balancer_full_name,
                },
                statistic="Sum",
                period=Duration.minutes(5),
            ),
            threshold=10,
            evaluation_periods=2,
            comparison_operator=cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            treat_missing_data=cloudwatch.TreatMissingData.NOT_BREACHING,
        ).add_alarm_action(cw_actions.SnsAction(self.alert_topic))

        # ECS Cluster alarms
        cloudwatch.Alarm(
            self,
            "ECSClusterHighCPU",
            alarm_name="nz-companies-register-ecs-cluster-high-cpu",
            alarm_description="ECS cluster CPU reservation is high",
            metric=cloudwatch.Metric(
                namespace="AWS/ECS",
                metric_name="CPUReservation",
                dimensions_map={
                    "ClusterName": compute_stack.cluster.cluster_name,
                },
                statistic="Average",
                period=Duration.minutes(5),
            ),
            threshold=80,
            evaluation_periods=2,
            comparison_operator=cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            treat_missing_data=cloudwatch.TreatMissingData.NOT_BREACHING,
        ).add_alarm_action(cw_actions.SnsAction(self.alert_topic))

    def create_database_alarms(self):
        """Create database-specific alarms"""
        # RDS CPU utilization
        cloudwatch.Alarm(
            self,
            "RDSHighCPU",
            alarm_name="nz-companies-register-rds-high-cpu",
            alarm_description="RDS CPU utilization is high",
            metric=cloudwatch.Metric(
                namespace="AWS/RDS",
                metric_name="CPUUtilization",
                dimensions_map={
                    "DBClusterIdentifier": "nz-companies-register-db",
                },
                statistic="Average",
                period=Duration.minutes(5),
            ),
            threshold=80,
            evaluation_periods=2,
            comparison_operator=cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            treat_missing_data=cloudwatch.TreatMissingData.NOT_BREACHING,
        ).add_alarm_action(cw_actions.SnsAction(self.alert_topic))

        # RDS connection count
        cloudwatch.Alarm(
            self,
            "RDSHighConnections",
            alarm_name="nz-companies-register-rds-high-connections",
            alarm_description="RDS connection count is high",
            metric=cloudwatch.Metric(
                namespace="AWS/RDS",
                metric_name="DatabaseConnections",
                dimensions_map={
                    "DBClusterIdentifier": "nz-companies-register-db",
                },
                statistic="Average",
                period=Duration.minutes(5),
            ),
            threshold=160,  # 80% of max_connections (200)
            evaluation_periods=2,
            comparison_operator=cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            treat_missing_data=cloudwatch.TreatMissingData.NOT_BREACHING,
        ).add_alarm_action(cw_actions.SnsAction(self.alert_topic))

        # DynamoDB throttling
        cloudwatch.Alarm(
            self,
            "DynamoDBThrottling",
            alarm_name="nz-companies-register-dynamodb-throttling",
            alarm_description="DynamoDB is experiencing throttling",
            metric=cloudwatch.Metric(
                namespace="AWS/DynamoDB",
                metric_name="ReadThrottledEvents",
                dimensions_map={
                    "TableName": "nz-companies-register-documents",
                },
                statistic="Sum",
                period=Duration.minutes(5),
            ),
            threshold=0,
            evaluation_periods=1,
            comparison_operator=cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            treat_missing_data=cloudwatch.TreatMissingData.NOT_BREACHING,
        ).add_alarm_action(cw_actions.SnsAction(self.alert_topic))

    def create_custom_metrics(self):
        """Create custom business metrics"""
        # Custom metrics will be published by the application
        # These are example alarms for business metrics

        # Company registration rate
        cloudwatch.Alarm(
            self,
            "LowRegistrationRate",
            alarm_name="nz-companies-register-low-registration-rate",
            alarm_description="Company registration rate is unusually low",
            metric=cloudwatch.Metric(
                namespace="NZCompaniesRegister/Business",
                metric_name="CompanyRegistrations",
                statistic="Sum",
                period=Duration.hours(1),
            ),
            threshold=5,  # Minimum expected registrations per hour
            evaluation_periods=2,
            comparison_operator=cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
            treat_missing_data=cloudwatch.TreatMissingData.BREACHING,
        ).add_alarm_action(cw_actions.SnsAction(self.alert_topic))

        # Search response time
        cloudwatch.Alarm(
            self,
            "SearchResponseTimeSLA",
            alarm_name="nz-companies-register-search-response-time-sla",
            alarm_description="Search response time exceeds SLA",
            metric=cloudwatch.Metric(
                namespace="NZCompaniesRegister/Performance",
                metric_name="SearchResponseTime",
                statistic="Average",
                period=Duration.minutes(5),
            ),
            threshold=500,  # 500ms SLA
            evaluation_periods=2,
            comparison_operator=cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            treat_missing_data=cloudwatch.TreatMissingData.NOT_BREACHING,
        ).add_alarm_action(cw_actions.SnsAction(self.alert_topic))

    def setup_dashboard_widgets(self, compute_stack: ComputeStack):
        """Setup dashboard widgets"""
        # Application performance widgets
        self.dashboard.add_widgets(
            cloudwatch.GraphWidget(
                title="Backend Service Metrics",
                left=[
                    cloudwatch.Metric(
                        namespace="AWS/ECS",
                        metric_name="CPUUtilization",
                        dimensions_map={
                            "ServiceName": compute_stack.backend_service.service_name,
                            "ClusterName": compute_stack.cluster.cluster_name,
                        },
                        statistic="Average",
                        period=Duration.minutes(5),
                    ),
                    cloudwatch.Metric(
                        namespace="AWS/ECS",
                        metric_name="MemoryUtilization",
                        dimensions_map={
                            "ServiceName": compute_stack.backend_service.service_name,
                            "ClusterName": compute_stack.cluster.cluster_name,
                        },
                        statistic="Average",
                        period=Duration.minutes(5),
                    ),
                ],
                right=[
                    cloudwatch.Metric(
                        namespace="AWS/ECS",
                        metric_name="RunningTaskCount",
                        dimensions_map={
                            "ServiceName": compute_stack.backend_service.service_name,
                            "ClusterName": compute_stack.cluster.cluster_name,
                        },
                        statistic="Average",
                        period=Duration.minutes(1),
                    ),
                ],
                width=12,
                height=6,
            ),
            cloudwatch.GraphWidget(
                title="ALB Metrics",
                left=[
                    cloudwatch.Metric(
                        namespace="AWS/ApplicationELB",
                        metric_name="TargetResponseTime",
                        dimensions_map={
                            "LoadBalancer": compute_stack.alb.load_balancer_full_name,
                        },
                        statistic="Average",
                        period=Duration.minutes(5),
                    ),
                ],
                right=[
                    cloudwatch.Metric(
                        namespace="AWS/ApplicationELB",
                        metric_name="RequestCount",
                        dimensions_map={
                            "LoadBalancer": compute_stack.alb.load_balancer_full_name,
                        },
                        statistic="Sum",
                        period=Duration.minutes(5),
                    ),
                ],
                width=12,
                height=6,
            ),
        )

        # Database performance widgets
        self.dashboard.add_widgets(
            cloudwatch.GraphWidget(
                title="Database Metrics",
                left=[
                    cloudwatch.Metric(
                        namespace="AWS/RDS",
                        metric_name="CPUUtilization",
                        dimensions_map={
                            "DBClusterIdentifier": "nz-companies-register-db",
                        },
                        statistic="Average",
                        period=Duration.minutes(5),
                    ),
                ],
                right=[
                    cloudwatch.Metric(
                        namespace="AWS/RDS",
                        metric_name="DatabaseConnections",
                        dimensions_map={
                            "DBClusterIdentifier": "nz-companies-register-db",
                        },
                        statistic="Average",
                        period=Duration.minutes(5),
                    ),
                ],
                width=12,
                height=6,
            ),
        )

        # Business metrics widgets
        self.dashboard.add_widgets(
            cloudwatch.GraphWidget(
                title="Business Metrics",
                left=[
                    cloudwatch.Metric(
                        namespace="NZCompaniesRegister/Business",
                        metric_name="CompanyRegistrations",
                        statistic="Sum",
                        period=Duration.hours(1),
                    ),
                    cloudwatch.Metric(
                        namespace="NZCompaniesRegister/Business",
                        metric_name="SearchRequests",
                        statistic="Sum",
                        period=Duration.minutes(5),
                    ),
                ],
                right=[
                    cloudwatch.Metric(
                        namespace="NZCompaniesRegister/Performance",
                        metric_name="SearchResponseTime",
                        statistic="Average",
                        period=Duration.minutes(5),
                    ),
                ],
                width=12,
                height=6,
            ),
        )

        # Add tags
        Tags.of(self).add("Project", "NZ Companies Register")
        Tags.of(self).add("Environment", "Production")
        Tags.of(self).add("Component", "Monitoring")
