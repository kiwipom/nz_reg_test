from aws_cdk import (
    Stack,
    aws_s3 as s3,
    aws_sns as sns,
    aws_sqs as sqs,
    aws_kms as kms,
    RemovalPolicy,
    Tags,
)
from constructs import Construct


class StorageStack(Stack):
    def __init__(self, scope: Construct, construct_id: str, **kwargs) -> None:
        super().__init__(scope, construct_id, **kwargs)

        # KMS key for encryption
        self.kms_key = kms.Key(
            self,
            "StorageKey",
            description="KMS key for NZ Companies Register storage encryption",
            enable_key_rotation=True,
            removal_policy=RemovalPolicy.RETAIN,
        )

        # S3 bucket for document storage
        self.document_bucket = s3.Bucket(
            self,
            "DocumentBucket",
            bucket_name="nz-companies-register-documents",
            versioned=True,
            encryption=s3.BucketEncryption.KMS,
            encryption_key=self.kms_key,
            block_public_access=s3.BlockPublicAccess.BLOCK_ALL,
            removal_policy=RemovalPolicy.RETAIN,
            lifecycle_rules=[
                s3.LifecycleRule(
                    id="TransitionToIA",
                    enabled=True,
                    transitions=[
                        s3.Transition(
                            storage_class=s3.StorageClass.INFREQUENT_ACCESS,
                            transition_after=30,
                        ),
                        s3.Transition(
                            storage_class=s3.StorageClass.GLACIER,
                            transition_after=90,
                        ),
                    ],
                ),
            ],
        )

        # S3 bucket for application logs
        self.logs_bucket = s3.Bucket(
            self,
            "LogsBucket",
            bucket_name="nz-companies-register-logs",
            versioned=False,
            encryption=s3.BucketEncryption.KMS,
            encryption_key=self.kms_key,
            block_public_access=s3.BlockPublicAccess.BLOCK_ALL,
            removal_policy=RemovalPolicy.RETAIN,
            lifecycle_rules=[
                s3.LifecycleRule(
                    id="DeleteOldLogs",
                    enabled=True,
                    expiration=365,  # Keep logs for 1 year
                ),
            ],
        )

        # SNS topic for notifications
        self.notification_topic = sns.Topic(
            self,
            "NotificationTopic",
            topic_name="nz-companies-register-notifications",
            display_name="NZ Companies Register Notifications",
            master_key=self.kms_key,
        )

        # SQS queue for processing notifications
        self.notification_queue = sqs.Queue(
            self,
            "NotificationQueue",
            queue_name="nz-companies-register-notifications",
            visibility_timeout_seconds=300,
            message_retention_period_seconds=1209600,  # 14 days
            encryption=sqs.QueueEncryption.KMS,
            encryption_master_key=self.kms_key,
        )

        # Dead letter queue for failed notifications
        self.notification_dlq = sqs.Queue(
            self,
            "NotificationDLQ",
            queue_name="nz-companies-register-notifications-dlq",
            visibility_timeout_seconds=300,
            message_retention_period_seconds=1209600,  # 14 days
            encryption=sqs.QueueEncryption.KMS,
            encryption_master_key=self.kms_key,
        )

        # Subscribe queue to topic
        self.notification_topic.add_subscription(
            sns.SqsSubscription(
                self.notification_queue,
                dead_letter_queue=self.notification_dlq,
            )
        )

        # SQS queue for annual return reminders
        self.reminder_queue = sqs.Queue(
            self,
            "ReminderQueue",
            queue_name="nz-companies-register-reminders",
            visibility_timeout_seconds=300,
            message_retention_period_seconds=1209600,  # 14 days
            encryption=sqs.QueueEncryption.KMS,
            encryption_master_key=self.kms_key,
        )

        # Dead letter queue for failed reminders
        self.reminder_dlq = sqs.Queue(
            self,
            "ReminderDLQ",
            queue_name="nz-companies-register-reminders-dlq",
            visibility_timeout_seconds=300,
            message_retention_period_seconds=1209600,  # 14 days
            encryption=sqs.QueueEncryption.KMS,
            encryption_master_key=self.kms_key,
        )

        # Add tags
        Tags.of(self).add("Project", "NZ Companies Register")
        Tags.of(self).add("Environment", "Production")
        Tags.of(self).add("Component", "Storage")
