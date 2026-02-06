#!/bin/bash
# =============================================================================
# LocalStack Initialization Script
# Creates S3 bucket and SQS queue for local development
# =============================================================================

echo "Initializing LocalStack resources..."

# Create S3 bucket for invoices
awslocal s3 mb s3://syncledger-invoices

# Create SQS queue for invoice processing
awslocal sqs create-queue --queue-name syncledger-invoice-queue

echo "LocalStack initialization complete!"
