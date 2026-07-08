# Vaults and Secrets

JavaQC is designed so cloud credentials do not have to be typed on every run.

## Goal

The intended workflow is:

- configure secrets once
- keep them in a vault or environment layer
- reuse them across runs
- keep the local simulator available without secrets

## Vault Priority

`VaultFactory` selects a vault in this order:

1. Azure Key Vault
2. HashiCorp Vault
3. environment variables

## Azure Key Vault

Set these environment variables:

```bash
export AZURE_KEY_VAULT_NAME="my-vault"
export AZURE_TENANT_ID="your-tenant-id"
export AZURE_CLIENT_ID="your-client-id"
export AZURE_CLIENT_SECRET="your-client-secret"
```

Store the Pasqal credentials:

```bash
az keyvault secret set --vault-name my-vault --name pasqal-client-id --value "YOUR_CLIENT_ID"
az keyvault secret set --vault-name my-vault --name pasqal-client-secret --value "YOUR_CLIENT_SECRET"
az keyvault secret set --vault-name my-vault --name pasqal-project-id --value "591eb05c-88c2-4ca5-b228-5fd91e64855f"
```

## HashiCorp Vault

Set:

```bash
export VAULT_ADDR="http://localhost:8200"
export VAULT_TOKEN="your-token"
```

Write the secret bundle:

```bash
vault kv put secret/pasqal/credentials \
  client_id="YOUR_CLIENT_ID" \
  client_secret="YOUR_CLIENT_SECRET" \
  project_id="591eb05c-88c2-4ca5-b228-5fd91e64855f"
```

## Environment Variable Fallback

If no vault is configured, the adapters can fall back to environment variables.

Examples:

```bash
export PASQAL_CLIENT_ID="your-client-id"
export PASQAL_CLIENT_SECRET="your-client-secret"
export PASQAL_PROJECT_ID="591eb05c-88c2-4ca5-b228-5fd91e64855f"
export IBM_QUANTUM_TOKEN="your-ibm-token"
export DWAVE_API_KEY="your-dwave-key"
```

## Development Notes

- Vault is the right production default.
- Secrets should be configured once, not retyped on each run.
- The local simulator should remain the zero-secret path.
- If you want a clone-and-run workflow, the repository should ship with safe defaults and a one-time bootstrap path, not manual key prompts on every execution.
