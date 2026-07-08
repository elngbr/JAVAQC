package com.gluonhq.strange.cloudlink.vault;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Azure Key Vault implementation for secure secret management.
 * 
 * Environment variables:
 * - AZURE_KEY_VAULT_NAME: Name of the Key Vault (e.g., my-vault)
 * - AZURE_TENANT_ID: Azure tenant ID
 * - AZURE_CLIENT_ID: Azure client ID
 * - AZURE_CLIENT_SECRET: Azure client secret
 */
public class AzureKeyVaultClient implements SecretVault {
    private static final String VAULT_URL_TEMPLATE = "https://%s.vault.azure.net";
    private static final String AUTH_URL = "https://login.microsoftonline.com/%s/oauth2/v2.0/token";
    
    private String vaultName;
    private String tenantId;
    private String clientId;
    private String clientSecret;
    private String cachedAccessToken;
    private long tokenExpiry;
    private HttpClient httpClient;
    
    public AzureKeyVaultClient() {
        this(System.getenv("AZURE_KEY_VAULT_NAME"),
             System.getenv("AZURE_TENANT_ID"),
             System.getenv("AZURE_CLIENT_ID"),
             System.getenv("AZURE_CLIENT_SECRET"));
    }
    
    public AzureKeyVaultClient(String vaultName, String tenantId, String clientId, String clientSecret) {
        this.vaultName = vaultName;
        this.tenantId = tenantId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.httpClient = HttpClient.newHttpClient();
    }
    
    @Override
    public String getSecret(String secretPath) throws Exception {
        if (!isAvailable()) {
            throw new IllegalStateException("Azure Key Vault is not configured");
        }
        
        String token = getAccessToken();
        String vaultUrl = String.format(VAULT_URL_TEMPLATE, vaultName);
        String uri = vaultUrl + "/secrets/" + secretPath + "?api-version=7.3";
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 404) {
            throw new Exception("Secret not found: " + secretPath);
        }
        if (response.statusCode() >= 400) {
            throw new Exception("Azure Key Vault error: " + response.body());
        }
        
        return response.body();
    }
    
    @Override
    public String getSecretField(String secretPath, String field) throws Exception {
        String response = getSecret(secretPath);
        return extractJsonField(response, field);
    }
    
    @Override
    public void putSecret(String secretPath, Map<String, String> data) throws Exception {
        if (!isAvailable()) {
            throw new IllegalStateException("Azure Key Vault is not configured");
        }
        
        String token = getAccessToken();
        String vaultUrl = String.format(VAULT_URL_TEMPLATE, vaultName);
        String uri = vaultUrl + "/secrets/" + secretPath + "?api-version=7.3";
        
        // Azure Key Vault stores a single value, combine all fields
        String value = data.values().iterator().next();
        String payload = "{\"value\":\"" + escapeJson(value) + "\"}";
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 400) {
            throw new Exception("Failed to store secret in Azure Key Vault: " + response.body());
        }
    }
    
    @Override
    public boolean isAvailable() {
        return vaultName != null && !vaultName.isEmpty()
            && tenantId != null && !tenantId.isEmpty()
            && clientId != null && !clientId.isEmpty()
            && clientSecret != null && !clientSecret.isEmpty();
    }
    
    private String getAccessToken() throws Exception {
        if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpiry) {
            return cachedAccessToken;
        }
        
        String url = String.format(AUTH_URL, tenantId);
        String body = String.format("grant_type=client_credentials&client_id=%s&client_secret=%s&scope=https://vault.azure.net/.default",
                clientId, escapeUrl(clientSecret));
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 400) {
            throw new Exception("Azure authentication failed: " + response.body());
        }
        
        cachedAccessToken = extractJsonField(response.body(), "access_token");
        long expiresIn = Long.parseLong(extractJsonField(response.body(), "expires_in"));
        tokenExpiry = System.currentTimeMillis() + (expiresIn * 1000) - 60000;
        
        return cachedAccessToken;
    }
    
    private String extractJsonField(String json, String field) {
        String searchStr = "\"" + field + "\":";
        int start = json.indexOf(searchStr);
        if (start == -1) return "";
        
        start += searchStr.length();
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        
        if (json.charAt(start) == '"') {
            int end = json.indexOf("\"", start + 1);
            return json.substring(start + 1, end);
        } else {
            int end = start;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
            return json.substring(start, end).trim();
        }
    }
    
    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r");
    }
    
    private String escapeUrl(String str) {
        try {
            return java.net.URLEncoder.encode(str, "UTF-8");
        } catch (Exception e) {
            return str;
        }
    }
}
