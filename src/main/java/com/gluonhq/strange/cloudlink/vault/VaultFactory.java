package com.gluonhq.strange.cloudlink.vault;

/**
 * Factory for creating SecretVault instances.
 * Automatically selects the appropriate vault implementation based on configuration.
 */
public class VaultFactory {
    
    public enum VaultType {
        HASHICORP_VAULT,
        AZURE_KEY_VAULT,
        ENVIRONMENT_VARIABLES // Fallback for development
    }
    
    /**
     * Create a vault instance based on available configuration.
     * Priority: Azure Key Vault > HashiCorp Vault > Environment Variables
     */
    public static SecretVault createVault() {
        // Check for Azure Key Vault
        if (isAzureKeyVaultConfigured()) {
            return new AzureKeyVaultClient();
        }
        
        // Check for HashiCorp Vault
        if (isHashiCorpVaultConfigured()) {
            return new HashiCorpVaultClient();
        }
        
        // Fallback to environment variables
        return new EnvironmentVariableVault();
    }
    
    /**
     * Create a vault instance of a specific type.
     */
    public static SecretVault createVault(VaultType type) {
        return switch (type) {
            case HASHICORP_VAULT -> new HashiCorpVaultClient();
            case AZURE_KEY_VAULT -> new AzureKeyVaultClient();
            case ENVIRONMENT_VARIABLES -> new EnvironmentVariableVault();
        };
    }
    
    private static boolean isAzureKeyVaultConfigured() {
        return System.getenv("AZURE_KEY_VAULT_NAME") != null
            && System.getenv("AZURE_TENANT_ID") != null
            && System.getenv("AZURE_CLIENT_ID") != null
            && System.getenv("AZURE_CLIENT_SECRET") != null;
    }
    
    private static boolean isHashiCorpVaultConfigured() {
        return System.getenv("VAULT_TOKEN") != null;
    }
}
