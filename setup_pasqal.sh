#!/bin/bash
# Pasqal Quick Setup Script
# This script sets example environment variables for Pasqal integration.
# Replace the placeholder values with your own local settings and do not commit real credentials.

export PASQAL_EMAIL="${PASQAL_EMAIL:-your-email@example.com}"
export PASQAL_CLIENT_ID="${PASQAL_CLIENT_ID:-your-client-id}"
export PASQAL_CLIENT_SECRET="${PASQAL_CLIENT_SECRET:-your-client-secret}"
export PASQAL_PROJECT_ID="${PASQAL_PROJECT_ID:-your-project-id}"

# Optional: Vault configuration (if using vaults)
# export VAULT_ADDR="http://localhost:8200"
# export VAULT_TOKEN="your-token"
# export AZURE_KEY_VAULT_NAME="your-vault"
# export AZURE_TENANT_ID="your-tenant"
# export AZURE_CLIENT_ID="your-client"
# export AZURE_CLIENT_SECRET="your-client-secret"

echo "✅ Pasqal environment variables set:"
echo "   Email:       $PASQAL_EMAIL"
echo "   Project ID:  $PASQAL_PROJECT_ID"
echo ""
echo "To use with the web server:"
echo "   cd site && python3 server.py"
echo ""
echo "To use in Java:"
echo "   PasqualAdapter adapter = new PasqualAdapter();"
