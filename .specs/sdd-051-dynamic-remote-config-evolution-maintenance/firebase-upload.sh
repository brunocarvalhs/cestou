#!/bin/bash

###############################################################################
# Firebase Remote Config Upload Script
#
# Usage:
#   ./firebase-upload.sh [staging|production] [version]
#
# Example:
#   ./firebase-upload.sh staging 1.0.0    # Upload to Staging Firebase
#   ./firebase-upload.sh production 1.0.0  # Upload to Production Firebase
#
# Requirements:
#   - Firebase CLI installed: npm install -g firebase-tools
#   - Authenticated: firebase login
#   - Google Cloud SDK: gcloud auth login
#
###############################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Arguments
PROJECT_ENV=${1:-staging}  # staging or production
VERSION=${2:-1.0.0}

# Validate arguments
if [[ "$PROJECT_ENV" != "staging" && "$PROJECT_ENV" != "production" ]]; then
    echo -e "${RED}Error: Invalid environment. Use 'staging' or 'production'${NC}"
    exit 1
fi

# Project IDs (adjust to your Firebase projects)
STAGING_PROJECT="cestou-staging"
PRODUCTION_PROJECT="cestou-prod"

if [[ "$PROJECT_ENV" == "staging" ]]; then
    PROJECT_ID=$STAGING_PROJECT
    echo -e "${YELLOW}📤 Uploading to Firebase Staging: $PROJECT_ID${NC}"
else
    PROJECT_ID=$PRODUCTION_PROJECT
    echo -e "${YELLOW}📤 Uploading to Firebase Production: $PROJECT_ID${NC}"
    echo -e "${RED}⚠️  PRODUCTION ENVIRONMENT - DOUBLE CHECK!${NC}"
    read -p "Are you sure? (type 'YES' to continue): " confirmation
    if [[ "$confirmation" != "YES" ]]; then
        echo "Aborted."
        exit 1
    fi
fi

# Create config JSON
CONFIG_JSON=$(cat <<'EOF'
{
  "version": {
    "versionNumber": "VERSION_PLACEHOLDER",
    "description": "Remote Config via sdd-051 governance"
  },
  "parameters": {
    "feat_ai_openrouter_enabled": {
      "description": "OpenRouter LLM provider enabled",
      "valueType": "BOOLEAN",
      "defaultValue": { "value": "true" }
    },
    "feat_ai_gemini_enabled": {
      "description": "Gemini Vision API provider enabled",
      "valueType": "BOOLEAN",
      "defaultValue": { "value": "true" }
    },
    "feat_proactive_ai_suggestions_enabled": {
      "description": "Proactive AI chips enabled",
      "valueType": "BOOLEAN",
      "defaultValue": { "value": "true" }
    },
    "feat_proactive_ai_actions_enabled": {
      "description": "Proactive AI actions (FABs) enabled",
      "valueType": "BOOLEAN",
      "defaultValue": { "value": "true" }
    },
    "ai_max_tokens": {
      "description": "Max tokens for AI response",
      "valueType": "NUMBER",
      "defaultValue": { "value": "1024" }
    },
    "ai_temperature": {
      "description": "Temperature for AI model (0.0-1.0)",
      "valueType": "STRING",
      "defaultValue": { "value": "0.7" }
    },
    "feat_auth_google_enabled": {
      "description": "Google Sign-In enabled",
      "valueType": "BOOLEAN",
      "defaultValue": { "value": "true" }
    },
    "feat_auth_email_enabled": {
      "description": "Email/Password auth enabled",
      "valueType": "BOOLEAN",
      "defaultValue": { "value": "true" }
    },
    "feat_app_functions_search_product_enabled": {
      "description": "SearchProduct AppFunction enabled",
      "valueType": "BOOLEAN",
      "defaultValue": { "value": "true" }
    },
    "feat_app_functions_add_multiple_enabled": {
      "description": "AddProductsMultiple AppFunction enabled",
      "valueType": "BOOLEAN",
      "defaultValue": { "value": "true" }
    },
    "feat_app_functions_list_all_enabled": {
      "description": "ListAllShoppingLists AppFunction enabled",
      "valueType": "BOOLEAN",
      "defaultValue": { "value": "true" }
    },
    "feat_app_functions_get_details_enabled": {
      "description": "GetShoppingDetails AppFunction enabled",
      "valueType": "BOOLEAN",
      "defaultValue": { "value": "true" }
    },
    "feat_app_functions_clear_purchased_enabled": {
      "description": "ClearPurchasedItems AppFunction enabled",
      "valueType": "BOOLEAN",
      "defaultValue": { "value": "true" }
    },
    "feat_analytics_ui_actions_enabled": {
      "description": "Analytics UI actions tracking enabled",
      "valueType": "BOOLEAN",
      "defaultValue": { "value": "true" }
    },
    "feat_feedback_nps_enabled": {
      "description": "NPS surveys enabled",
      "valueType": "BOOLEAN",
      "defaultValue": { "value": "true" }
    },
    "feat_budget_alerts_enabled": {
      "description": "Budget alerts enabled",
      "valueType": "BOOLEAN",
      "defaultValue": { "value": "true" }
    },
    "budget_alert_threshold_percent": {
      "description": "Budget alert threshold (%)",
      "valueType": "NUMBER",
      "defaultValue": { "value": "80" }
    },
    "shopping_max_products_per_list": {
      "description": "Max products per list",
      "valueType": "NUMBER",
      "defaultValue": { "value": "100" }
    },
    "feedback_nps_cooldown_days": {
      "description": "Cooldown between NPS surveys (days)",
      "valueType": "NUMBER",
      "defaultValue": { "value": "90" }
    }
  }
}
EOF
)

# Replace version placeholder
CONFIG_JSON="${CONFIG_JSON//VERSION_PLACEHOLDER/$VERSION}"

# Save to temp file
TEMP_CONFIG="/tmp/remote_config_$VERSION.json"
echo "$CONFIG_JSON" > "$TEMP_CONFIG"

echo -e "${GREEN}✓ Config JSON created${NC}"

# Upload using Firebase Admin SDK
echo -e "${YELLOW}📤 Uploading to Firebase...${NC}"

# Use firebase-admin CLI or gcloud
gcloud --project="$PROJECT_ID" database --version=NONE remoteconfig:push "$TEMP_CONFIG" || \
firebase remoteconfig:put "$TEMP_CONFIG" --project="$PROJECT_ID"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Successfully uploaded to $PROJECT_ENV${NC}"
    echo -e "${GREEN}✓ Version: $VERSION${NC}"
    echo -e "${YELLOW}📝 Next steps:${NC}"
    echo "1. Firebase Console → Remote Config (${PROJECT_ENV})"
    echo "2. Review parameters"
    echo "3. Create rollout conditions (canary: random_percent <= 5)"
    echo "4. Publish"
    rm -f "$TEMP_CONFIG"
else
    echo -e "${RED}✗ Upload failed${NC}"
    echo "Troubleshooting:"
    echo "1. firebase login"
    echo "2. gcloud auth login"
    echo "3. Check PROJECT_ID is correct"
    exit 1
fi
