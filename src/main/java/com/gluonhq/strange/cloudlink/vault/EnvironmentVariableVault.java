package com.gluonhq.strange.cloudlink.vault;

import java.util.Map;

/**
 * Fallback vault implementation that reads from environment variables.
 * Suitable for development and local testing only.
 * DO NOT use in production!
 */
public class EnvironmentVariableVault implements SecretVault {
    
    @Override
    public String getSecret(String secretPath) throws Exception {
        // For env var vault, secretPath is the env var name
        String value = System.getenv(secretPath);
        if (value == null) {
            throw new Exception("Environment variable not found: " + secretPath);
        }
        return value;
    }
    
    @Override
    public String getSecretField(String secretPath, String field) throws Exception {
        // For env var vault, treat as secret path with optional field suffix
        String envVarName = secretPath.replace("/", "_").toUpperCase();
        if (!field.isEmpty()) {
            envVarName = envVarName + "_" + field.toUpperCase();
        }
        
        String value = System.getenv(envVarName);
        if (value == null) {
            throw new Exception("Environment variable not found: " + envVarName);
        }
        return value;
    }
    
    @Override
    public void putSecret(String secretPath, Map<String, String> data) throws Exception {
        System.err.println("Warning: EnvironmentVariableVault does not support storing secrets. Use a real vault for production.");
        throw new UnsupportedOperationException("EnvironmentVariableVault does not support storing secrets");
    }
    
    @Override
    public boolean isAvailable() {
        return true; // Always available (fallback)
    }
}
