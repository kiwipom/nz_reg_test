import pytest
from aws_cdk import App
from aws_cdk.assertions import Template, Match
from stacks.storage_stack import StorageStack


class TestStorageStack:
    @pytest.fixture
    def template(self):
        app = App()
        stack = StorageStack(app, "TestStorageStack")
        return Template.from_stack(stack)

    def test_kms_key_created(self, template):
        """Test that KMS key is created with key rotation"""
        template.has_resource_properties(
            "AWS::KMS::Key",
            {
                "Description": "KMS key for NZ Companies Register storage encryption",
                "EnableKeyRotation": True,
            },
        )

    def test_document_bucket_created(self, template):
        """Test that document S3 bucket is created"""
        template.has_resource_properties(
            "AWS::S3::Bucket",
            {
                "BucketName": "nz-companies-register-documents",
                "VersioningConfiguration": {
                    "Status": "Enabled",
                },
                "PublicAccessBlockConfiguration": {
                    "BlockPublicAcls": True,
                    "BlockPublicPolicy": True,
                    "IgnorePublicAcls": True,
                    "RestrictPublicBuckets": True,
                },
            },
        )

    def test_logs_bucket_created(self, template):
        """Test that logs S3 bucket is created"""
        template.has_resource_properties(
            "AWS::S3::Bucket",
            {
                "BucketName": "nz-companies-register-logs",
                "PublicAccessBlockConfiguration": {
                    "BlockPublicAcls": True,
                    "BlockPublicPolicy": True,
                    "IgnorePublicAcls": True,
                    "RestrictPublicBuckets": True,
                },
            },
        )

    def test_bucket_encryption_configured(self, template):
        """Test that S3 buckets are encrypted"""
        template.has_resource_properties(
            "AWS::S3::Bucket",
            {
                "BucketEncryption": {
                    "ServerSideEncryptionConfiguration": [
                        {
                            "ServerSideEncryptionByDefault": {
                                "SSEAlgorithm": "aws:kms",
                            },
                        }
                    ],
                },
            },
        )

    def test_bucket_lifecycle_configured(self, template):
        """Test that S3 bucket lifecycle rules are configured"""
        template.has_resource_properties(
            "AWS::S3::Bucket",
            {
                "LifecycleConfiguration": {
                    "Rules": [
                        {
                            "Id": "TransitionToIA",
                            "Status": "Enabled",
                            "Transitions": [
                                {
                                    "StorageClass": "STANDARD_IA",
                                    "TransitionInDays": 30,
                                },
                                {
                                    "StorageClass": "GLACIER",
                                    "TransitionInDays": 90,
                                },
                            ],
                        },
                    ],
                },
            },
        )

    def test_sns_topic_created(self, template):
        """Test that SNS topic is created"""
        template.has_resource_properties(
            "AWS::SNS::Topic",
            {
                "TopicName": "nz-companies-register-notifications",
                "DisplayName": "NZ Companies Register Notifications",
            },
        )

    def test_sqs_queues_created(self, template):
        """Test that SQS queues are created"""
        # Main notification queue
        template.has_resource_properties(
            "AWS::SQS::Queue",
            {
                "QueueName": "nz-companies-register-notifications",
                "VisibilityTimeout": 300,
                "MessageRetentionPeriod": 1209600,  # 14 days
            },
        )

        # Dead letter queue
        template.has_resource_properties(
            "AWS::SQS::Queue",
            {
                "QueueName": "nz-companies-register-notifications-dlq",
                "VisibilityTimeout": 300,
                "MessageRetentionPeriod": 1209600,  # 14 days
            },
        )

        # Reminder queue
        template.has_resource_properties(
            "AWS::SQS::Queue",
            {
                "QueueName": "nz-companies-register-reminders",
                "VisibilityTimeout": 300,
                "MessageRetentionPeriod": 1209600,  # 14 days
            },
        )

    def test_sqs_encryption_configured(self, template):
        """Test that SQS queues are encrypted"""
        template.has_resource_properties(
            "AWS::SQS::Queue",
            {
                "KmsMasterKeyId": Match.any_value(),
            },
        )

    def test_sns_subscription_created(self, template):
        """Test that SNS subscription to SQS is created"""
        template.has_resource_properties(
            "AWS::SNS::Subscription",
            {
                "Protocol": "sqs",
            },
        )

    def test_dead_letter_queue_configured(self, template):
        """Test that dead letter queue is configured"""
        # Check that DLQ is configured through SNS subscription
        template.has_resource_properties(
            "AWS::SNS::Subscription",
            {
                "RedrivePolicy": Match.any_value(),
            },
        )

    def test_tags_applied(self, template):
        """Test that proper tags are applied"""
        # Test each tag individually to be more resilient to tag ordering
        template.has_resource_properties(
            "AWS::S3::Bucket",
            {
                "Tags": Match.array_with(
                    [{"Key": "Project", "Value": "NZ Companies Register"}]
                )
            },
        )
        template.has_resource_properties(
            "AWS::S3::Bucket",
            {"Tags": Match.array_with([{"Key": "Environment", "Value": "Production"}])},
        )
        template.has_resource_properties(
            "AWS::S3::Bucket",
            {"Tags": Match.array_with([{"Key": "Component", "Value": "Storage"}])},
        )

    def test_security_best_practices(self, template):
        """Test that security best practices are followed"""
        # KMS key rotation enabled
        template.has_resource_properties(
            "AWS::KMS::Key",
            {
                "EnableKeyRotation": True,
            },
        )

        # S3 public access blocked
        template.has_resource_properties(
            "AWS::S3::Bucket",
            {
                "PublicAccessBlockConfiguration": {
                    "BlockPublicAcls": True,
                    "BlockPublicPolicy": True,
                    "IgnorePublicAcls": True,
                    "RestrictPublicBuckets": True,
                },
            },
        )

    def test_message_retention_configured(self, template):
        """Test that message retention is properly configured"""
        template.has_resource_properties(
            "AWS::SQS::Queue",
            {
                "MessageRetentionPeriod": 1209600,  # 14 days
            },
        )
