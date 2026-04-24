#!/usr/bin/env bash
set -euo pipefail

SERVICE="ieee-checker"       # Cloud Run service name
REGION="asia-south1"
PROJECT_ID="pdf-format-checker"

echo "Deploying $SERVICE to $REGION in project $PROJECT_ID..."

gcloud config set project "$PROJECT_ID"

gcloud run deploy "$SERVICE" \
  --source . \
  --region="$REGION" \
  --allow-unauthenticated \
  --memory=512Mi \
  --min-instances=0 \
  --max-instances=1 \
  --concurrency=10
