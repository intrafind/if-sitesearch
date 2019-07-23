#!/usr/bin/env sh

# Execute this to set all required environment variables, in order to launch service.

export SIS_API_SERVICE_URL=http://localhost:8001 # for local development only, should point to the actual API endpoint in production
export SIS_SERVICE_HOST=main.sitesearch.cloud
export ADMIN_SITE_SECRET=82ba4106-397b-11e8-9155-0a0027000009
export RECAPTCHA_SITE_SECRET=82ba4106-397b-11e8-9155-0a0027000009 # deprecated, remove later on, also from CI
export INVISIBLE_RECAPTCHA_SITE_SECRET=82ba4106-397b-11e8-9155-0a0027000009
export SECURITY_OAUTH2_CLIENT_CLIENT_SECRET=82ba4106-397b-11e8-9155-0a0027000009
export SPRING_SECURITY_USER_PASSWORD=82ba4106-397b-11e8-9155-0a0027000009
export SERVICE_SECRET=82ba4106-397b-11e8-9155-0a0027000009 # for iFinder Core access
export WOO_COMMERCE_CONSUMER_KEY=82ba4106-397b-11e8-9155-0a0027000009 # for iFinder Core access
export WOO_COMMERCE_CONSUMER_SECRET=82ba4106-397b-11e8-9155-0a0027000009 # for iFinder Core access
export DEV_SKIP_FLAG=true # skip some oAuth2 checks in integration tests to enable additional test scenarios
