#!/usr/bin/env python3
import aws_cdk as cdk
from stacks.vpc_stack import VpcStack
from stacks.database_stack import DatabaseStack
from stacks.storage_stack import StorageStack
from stacks.compute_stack import ComputeStack
from stacks.monitoring_stack import MonitoringStack
from stacks.security_stack import SecurityStack

app = cdk.App()

env = cdk.Environment(
    account=app.node.try_get_context("account"),
    region=app.node.try_get_context("region") or "ap-southeast-2",
)

# Core infrastructure
vpc_stack = VpcStack(app, "NzCompaniesVpc", env=env)
security_stack = SecurityStack(app, "NzCompaniesSecurity", env=env)
database_stack = DatabaseStack(app, "NzCompaniesDatabase", vpc=vpc_stack.vpc, env=env)
storage_stack = StorageStack(app, "NzCompaniesStorage", env=env)

# Application infrastructure
compute_stack = ComputeStack(
    app,
    "NzCompaniesCompute",
    vpc=vpc_stack.vpc,
    database=database_stack.database,
    document_bucket=storage_stack.document_bucket,
    env=env,
)

# Monitoring infrastructure
monitoring_stack = MonitoringStack(
    app, "NzCompaniesMonitoring", compute_stack=compute_stack, env=env
)

app.synth()
