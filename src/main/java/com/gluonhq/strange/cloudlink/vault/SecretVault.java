package com.gluonhq.strange.cloudlink.vault;

import java.util.Map;

/**
 * Interface for secure secret management.
 * Implementations can use HashiCorp Vault, Azure Key Vault, AWS Secrets Manager, etc.
 */
public interface SecretVault {
    
    /**
     * Retrieve a secret from the vault.
     * @param secretPath Path to the secret in the vault
     * @return The secret value
     * @throws Exception if secret not found or vault is unavailable
     */
    String getSecret(String secretPath) throws Exception;
    
    /**
     * Retrieve a specific field from a secret object.
     * @param secretPath Path to the secret in the vault
     * @param field Field name within the secret
     * @return The field value
     * @throws Exception if secret or field not found
     */
    String getSecretField(String secretPath, String field) throws Exception;
    
    /**
     * Store a secret in the vault.
     * @param secretPath Path to store the secret
     * @param data The secret data (as key-value pairs)
     * @throws Exception if storage fails
     */
    void putSecret(String secretPath, Map<String, String> data) throws Exception;
    
    /**
     * Check if vault is connected and accessible.
     * @return true if vault is available
     */
    boolean isAvailable();
}
