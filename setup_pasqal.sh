#!/bin/bash
# Pasqal Quick Setup Script
# This script sets environment variables for Pasqual quantum computing integration

# Pasqual credentials (hardcoded for development)
export PASQAL_EMAIL="elenaeft07@gmail.com"
export PASQAL_CLIENT_ID="${PASQAL_CLIENT_ID:-demo-client-id}"
export PASQAL_CLIENT_SECRET="${PASQAL_CLIENT_SECRET:-demo-client-secret}"
export PASQAL_PROJECT_ID="591eb05c-88c2-4ca5-b228-5fd91e64855f"

# Optional: Vault configuration (if using vaults)
# export VAULT_ADDR="http://localhost:8200"
# export VAULT_TOKEN="your-token"
# export AZURE_KEY_VAULT_NAME="your-vault"
# export AZURE_TENANT_ID="your-tenant"
# export AZURE_CLIENT_ID="your-client"
# export AZURE_CLIENT_SECRET="your-secret"

echo "✅ Pasqal environment variables set:"
echo "   Email:       $PASQAL_EMAIL"
echo "   Project ID:  $PASQAL_PROJECT_ID"
echo ""
echo "To use with the web server:"
echo "   cd site && python3 server.py"
echo ""
echo "To use in Java:"
echo "   PasqualAdapter adapter = new PasqualAdapter();"
