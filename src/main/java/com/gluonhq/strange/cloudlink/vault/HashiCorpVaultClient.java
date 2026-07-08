package com.gluonhq.strange.cloudlink.vault;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * HashiCorp Vault implementation for secure secret management.
 * 
 * Environment variables:
 * - VAULT_ADDR: Vault server address (e.g., http://localhost:8200)
 * - VAULT_TOKEN: Vault authentication token
 * - VAULT_NAMESPACE: (Optional) Vault namespace
 */
public class HashiCorpVaultClient implements SecretVault {
    private static final String DEFAULT_VAULT_ADDR = "http://localhost:8200";
    
    private String vaultAddr;
    private String vaultToken;
    private String vaultNamespace;
    private HttpClient httpClient;
    
    public HashiCorpVaultClient() {
        this(System.getenv("VAULT_ADDR"), System.getenv("VAULT_TOKEN"), System.getenv("VAULT_NAMESPACE"));
    }
    
    public HashiCorpVaultClient(String vaultAddr, String vaultToken, String vaultNamespace) {
        this.vaultAddr = vaultAddr != null ? vaultAddr : DEFAULT_VAULT_ADDR;
        this.vaultToken = vaultToken;
        this.vaultNamespace = vaultNamespace != null ? vaultNamespace : "";
        this.httpClient = HttpClient.newHttpClient();
    }
    
    @Override
    public String getSecret(String secretPath) throws Exception {
        if (!isAvailable()) {
            throw new IllegalStateException("Vault is not available or not configured");
        }
        
        String uri = vaultAddr + "/v1/secret/data/" + secretPath;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("X-Vault-Token", vaultToken)
                .GET()
                .build();
        
        if (!vaultNamespace.isEmpty()) {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .header("X-Vault-Token", vaultToken)
                    .header("X-Vault-Namespace", vaultNamespace)
                    .GET()
                    .build();
        }
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 404) {
            throw new Exception("Secret not found: " + secretPath);
        }
        if (response.statusCode() >= 400) {
            throw new Exception("Vault error: " + response.body());
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
            throw new IllegalStateException("Vault is not available or not configured");
        }
        
        String payload = mapToJson(data);
        String uri = vaultAddr + "/v1/secret/data/" + secretPath;
        
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("X-Vault-Token", vaultToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload));
        
        if (!vaultNamespace.isEmpty()) {
            builder.header("X-Vault-Namespace", vaultNamespace);
        }
        
        HttpRequest request = builder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 400) {
            throw new Exception("Failed to store secret: " + response.body());
        }
    }
    
    @Override
    public boolean isAvailable() {
        return vaultToken != null && !vaultToken.isEmpty();
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
    
    private String mapToJson(Map<String, String> data) {
        StringBuilder sb = new StringBuilder("{\"data\":{");
        boolean first = true;
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":\"").append(escapeJson(entry.getValue())).append("\"");
            first = false;
        }
        sb.append("}}");
        return sb.toString();
    }
    
    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r");
    }
}
