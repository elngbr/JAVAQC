# Vault Configuration Guide

## Overview

The Pasqal Adapter now uses a secure vault system to manage sensitive credentials. The system automatically detects and uses the best available vault implementation.

## Vault Priority Order

1. **Azure Key Vault** (Production recommended)
2. **HashiCorp Vault** (Enterprise ready)
3. **Environment Variables** (Development only - fallback)

---

## Setup Options

### Option 1: Azure Key Vault (Recommended for Production)

#### Prerequisites

- Azure subscription
- Key Vault instance created

#### Configuration

Set these environment variables:

```bash
export AZURE_KEY_VAULT_NAME="my-vault"
export AZURE_TENANT_ID="your-tenant-id"
export AZURE_CLIENT_ID="your-client-id"
export AZURE_CLIENT_SECRET="your-client-secret"
```

#### Store Pasqal Credentials

```bash
# Via Azure CLI
az keyvault secret set --vault-name my-vault --name pasqal-client-id --value "YOUR_CLIENT_ID"
az keyvault secret set --vault-name my-vault --name pasqal-client-secret --value "YOUR_CLIENT_SECRET"
az keyvault secret set --vault-name my-vault --name pasqal-project-id --value "591eb05c-88c2-4ca5-b228-5fd91e64855f"
```

#### Usage

```java
PasqualAdapter adapter = new PasqualAdapter(); // Automatically uses Azure Key Vault
```

---

### Option 2: HashiCorp Vault (Enterprise Ready)

#### Prerequisites

- HashiCorp Vault server running
- Auth token

#### Configuration

Set these environment variables:

```bash
export VAULT_ADDR="http://localhost:8200"
export VAULT_TOKEN="your-token"
export VAULT_NAMESPACE=""  # Optional, leave empty if not using namespaces
```

#### Store Pasqal Credentials

```bash
vault kv put secret/pasqal/credentials \
  client_id="YOUR_CLIENT_ID" \
  client_secret="YOUR_CLIENT_SECRET" \
  project_id="591eb05c-88c2-4ca5-b228-5fd91e64855f"
```

#### Usage

```java
PasqualAdapter adapter = new PasqualAdapter(); // Automatically uses HashiCorp Vault
```

---

### Option 3: Environment Variables (Development Only)

#### Configuration

Set these environment variables:

```bash
export PASQAL_CLIENT_ID="your-client-id"
export PASQAL_CLIENT_SECRET="your-client-secret"
export PASQAL_PROJECT_ID="591eb05c-88c2-4ca5-b228-5fd91e64855f"
```

#### Usage

```java
PasqualAdapter adapter = new PasqualAdapter(); // Uses environment variables
```

---

## Using Custom Vault Implementation

You can provide your own vault implementation:

```java
import com.gluonhq.strange.cloudlink.vault.SecretVault;

// Implement SecretVault interface
class MyCustomVault implements SecretVault {
    // ... implementation
}

// Use it with the adapter
MyCustomVault vault = new MyCustomVault();
PasqualAdapter adapter = new PasqualAdapter(vault);
```

---

## Security Best Practices

1. **Never commit secrets** to version control
2. **Use production vaults** (Azure Key Vault or HashiCorp Vault) in production environments
3. **Rotate credentials** regularly
4. **Limit access** - grant minimum required permissions
5. **Use managed identities** (Azure) or service accounts (HashiCorp) when possible
6. **Enable audit logging** on your vault

---

## Troubleshooting

### Azure Key Vault Issues

```
Error: "Azure Key Vault is not configured"
→ Check AZURE_KEY_VAULT_NAME, AZURE_TENANT_ID, AZURE_CLIENT_ID, AZURE_CLIENT_SECRET
```

### HashiCorp Vault Issues

```
Error: "Vault is not available or not configured"
→ Check VAULT_ADDR and VAULT_TOKEN
→ Verify vault server is running and accessible
```

### Environment Variables Fallback

```
Error: "Environment variable not found: PASQAL_CLIENT_ID"
→ No vault found, falling back to env vars
→ Set PASQAL_CLIENT_ID, PASQAL_CLIENT_SECRET, PASQAL_PROJECT_ID
```
