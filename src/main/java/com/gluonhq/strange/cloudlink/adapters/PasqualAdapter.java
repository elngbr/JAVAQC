package com.gluonhq.strange.cloudlink.adapters;

import com.gluonhq.strange.cloudlink.providers.QuantumCloudProvider;
import com.gluonhq.strange.cloudlink.vault.SecretVault;
import com.gluonhq.strange.cloudlink.vault.VaultFactory;
import org.redfx.strange.Program;
import org.redfx.strange.Result;
import org.redfx.strange.print.TextPrinter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Adapter for Pasqal quantum computing platform (neutral atoms).
 * Pasqal offers neutral atom-based quantum computers.
 * 
 * Supports multiple credential methods:
 * 1. OAuth2 via vault (HashiCorp Vault, Azure Key Vault)
 * 2. Email-based authentication (PASQAL_EMAIL env var)
 * 3. Direct credentials (clientId, clientSecret, projectId)
 * 
 * Environment variables:
 * - PASQAL_EMAIL: Email address for user identification (e.g.,
 * elenaeft07@gmail.com)
 * - PASQAL_CLIENT_ID: OAuth2 client ID
 * - PASQAL_CLIENT_SECRET: OAuth2 client secret
 * - PASQAL_PROJECT_ID: Project ID (e.g., 591eb05c-88c2-4ca5-b228-5fd91e64855f)
 */
public class PasqualAdapter implements QuantumCloudProvider {
    private static final String AUTH_URL = "https://pasqal.eu.auth0.com/oauth/token";
    private static final String API_BASE_URL = "https://apis.pasqal.cloud/core/v1";

    private String clientId;
    private String clientSecret;
    private String projectId;
    private String cachedAccessToken;
    private long tokenExpiry;
    private SecretVault vault;

    public PasqualAdapter() {
        this.vault = VaultFactory.createVault();
        initializeCredentials();
    }

    public PasqualAdapter(String clientId, String clientSecret, String projectId) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.projectId = projectId != null ? projectId : "591eb05c-88c2-4ca5-b228-5fd91e64855f";
        this.vault = null;
    }

    /**
     * Create adapter with custom vault implementation.
     */
    public PasqualAdapter(SecretVault vault) {
        this.vault = vault;
        initializeCredentials();
    }

    private void initializeCredentials() {
        // First, check for environment variables (highest priority)
        String envEmail = System.getenv("PASQAL_EMAIL");
        String envClientId = System.getenv("PASQAL_CLIENT_ID");
        String envClientSecret = System.getenv("PASQAL_CLIENT_SECRET");
        String envProjectId = System.getenv("PASQAL_PROJECT_ID");

        if (envClientId != null && !envClientId.isEmpty()) {
            this.clientId = envClientId;
        }
        if (envClientSecret != null && !envClientSecret.isEmpty()) {
            this.clientSecret = envClientSecret;
        }
        if (envProjectId != null && !envProjectId.isEmpty()) {
            this.projectId = envProjectId;
        }

        // If email is provided, log it for reference
        if (envEmail != null && !envEmail.isEmpty()) {
            System.out.println("Using Pasqal email: " + envEmail);
        }

        // Then, try vault if available
        if (vault != null && vault.isAvailable()) {
            try {
                this.clientId = vault.getSecretField("pasqal/credentials", "client_id");
            } catch (Exception e) {
                System.err.println("Failed to load Pasqal client ID from vault: " + e.getMessage());
            }

            try {
                this.clientSecret = vault.getSecretField("pasqal/credentials", "client_secret");
            } catch (Exception e) {
                System.err.println("Failed to load Pasqal client secret from vault: " + e.getMessage());
            }

            try {
                this.projectId = vault.getSecretField("pasqal/credentials", "project_id");
            } catch (Exception e) {
                this.projectId = "591eb05c-88c2-4ca5-b228-5fd91e64855f"; // Use default
            }
        }
    }

    @Override
    public Result submitProgram(Program program, int shots) throws Exception {
        Map<String, Integer> counts = getMeasurementCounts(program, shots);
        return convertCountsToResult(program, counts);
    }

    @Override
    public Map<String, Integer> getMeasurementCounts(Program program, int shots) throws Exception {
        if (!isAvailable()) {
            throw new IllegalStateException("Pasqal credentials not configured");
        }

        try {
            // 1. Get access token
            String accessToken = getAccessToken();

            // 2. Convert program to Pasqal Pulser format (simplified for now)
            String pulserCode = convertToPulserFormat(program, shots);

            // 3. Create batch request
            String batchPayload = String.format("""
                    {
                        "project_id": "%s",
                        "content": "%s"
                    }
                    """, projectId, escapeJson(pulserCode));

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest batchRequest = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_URL + "/batches"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(batchPayload))
                    .build();

            HttpResponse<String> batchResponse = client.send(batchRequest, HttpResponse.BodyHandlers.ofString());

            if (batchResponse.statusCode() >= 400) {
                throw new Exception("Pasqal API error: " + batchResponse.body());
            }

            // 4. Parse batch ID and poll for results
            String batchId = extractFieldFromJson(batchResponse.body(), "id");
            Map<String, Integer> results = pollForResults(client, accessToken, batchId, shots);

            return results;
        } catch (Exception e) {
            throw new Exception("Failed to submit program to Pasqal: " + e.getMessage(), e);
        }
    }

    private String getAccessToken() throws Exception {
        // Return cached token if still valid
        if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpiry) {
            return cachedAccessToken;
        }

        String authBody = String.format("""
                {
                    "grant_type": "client_credentials",
                    "client_id": "%s",
                    "client_secret": "%s",
                    "audience": "https://apis.pasqal.cloud"
                }
                """, clientId, clientSecret);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest authRequest = HttpRequest.newBuilder()
                .uri(URI.create(AUTH_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(authBody))
                .build();

        HttpResponse<String> authResponse = client.send(authRequest, HttpResponse.BodyHandlers.ofString());

        if (authResponse.statusCode() >= 400) {
            throw new Exception("Auth0 authentication failed: " + authResponse.body());
        }

        cachedAccessToken = extractFieldFromJson(authResponse.body(), "access_token");
        long expiresIn = Long.parseLong(extractFieldFromJson(authResponse.body(), "expires_in"));
        tokenExpiry = System.currentTimeMillis() + (expiresIn * 1000) - 60000; // 1 min buffer

        return cachedAccessToken;
    }

    private Map<String, Integer> pollForResults(HttpClient client, String accessToken, String batchId, int shots)
            throws Exception {
        int maxAttempts = 60;
        int attempt = 0;

        while (attempt < maxAttempts) {
            HttpRequest statusRequest = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_URL + "/batches/" + batchId))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> statusResponse = client.send(statusRequest, HttpResponse.BodyHandlers.ofString());
            String status = extractFieldFromJson(statusResponse.body(), "status");

            if ("DONE".equals(status)) {
                // Parse results
                return extractMeasurementCounts(statusResponse.body(), shots);
            } else if ("FAILED".equals(status) || "CANCELLED".equals(status)) {
                throw new Exception("Batch execution failed with status: " + status);
            }

            Thread.sleep(2000); // Wait 2 seconds before polling again
            attempt++;
        }

        throw new Exception("Batch execution timeout");
    }

    private Map<String, Integer> extractMeasurementCounts(String response, int shots) {
        // Simplified: generate dummy results for now
        // In production, parse actual measurement results from Pasqal response
        Map<String, Integer> counts = new HashMap<>();
        counts.put("00", shots / 2);
        counts.put("11", shots / 2);
        return counts;
    }

    private String convertToPulserFormat(Program program, int shots) {
        // Simplified Pulser code generation
        StringBuilder sb = new StringBuilder();
        sb.append("import pulser\n");
        sb.append("from pulser import Pulse, Register, Sequence\n\n");
        sb.append("# Convert from Strange program: ").append(program.toString()).append("\n");
        sb.append("# Number of shots: ").append(shots).append("\n");
        return sb.toString();
    }

    private String extractFieldFromJson(String json, String field) {
        String searchStr = "\"" + field + "\":";
        int start = json.indexOf(searchStr);
        if (start == -1)
            return "";

        start += searchStr.length();
        // Skip whitespace
        while (start < json.length() && Character.isWhitespace(json.charAt(start)))
            start++;

        if (json.charAt(start) == '"') {
            int end = json.indexOf("\"", start + 1);
            return json.substring(start + 1, end);
        } else {
            int end = start;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}')
                end++;
            return json.substring(start, end).trim();
        }
    }

    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    @Override
    public String getProviderName() {
        return "Pasqal";
    }

    @Override
    public boolean isAvailable() {
        return clientId != null && !clientId.isEmpty()
                && clientSecret != null && !clientSecret.isEmpty()
                && projectId != null && !projectId.isEmpty();
    }

    private Result convertCountsToResult(Program program, Map<String, Integer> counts) {
        int nqubits = program.getNumberQubits();
        Result result = new Result(nqubits, 0);
        // Populate result with measurement counts
        return result;
    }
}
