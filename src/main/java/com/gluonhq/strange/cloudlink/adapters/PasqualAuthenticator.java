package com.gluonhq.strange.cloudlink.adapters;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

/**
 * Utility for Pasqual email-based authentication.
 * Can generate OAuth2 tokens or basic auth headers from email credentials.
 */
public class PasqualAuthenticator {
    private static final String AUTH_URL = "https://pasqal.eu.auth0.com/oauth/token";
    private static final String USER_INFO_URL = "https://apis.pasqal.cloud/api/v1/auth/info";

    private String email;
    private String password;
    private HttpClient httpClient;

    public PasqualAuthenticator(String email, String password) {
        this.email = email;
        this.password = password;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Get user ID via basic auth from Pasqal API.
     */
    public String getUserId() throws Exception {
        String basicAuth = Base64.getEncoder().encodeToString((email + ":" + password).getBytes());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(USER_INFO_URL))
                .header("Authorization", "Basic " + basicAuth)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401) {
            throw new Exception("Pasqal authentication failed: Invalid email or password");
        }
        if (response.statusCode() >= 400) {
            throw new Exception("Pasqal API error: " + response.body());
        }

        return extractJsonField(response.body(), "user_id");
    }

    /**
     * Get OAuth2 token for API access.
     */
    public String getOAuthToken(String clientId, String clientSecret) throws Exception {
        String authBody = String.format("""
                {
                    "grant_type": "client_credentials",
                    "client_id": "%s",
                    "client_secret": "%s",
                    "audience": "https://apis.pasqal.cloud"
                }
                """, escapeJson(clientId), escapeJson(clientSecret));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AUTH_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(authBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new Exception("OAuth2 authentication failed: " + response.body());
        }

        return extractJsonField(response.body(), "access_token");
    }

    private String extractJsonField(String json, String field) {
        String searchStr = "\"" + field + "\":";
        int start = json.indexOf(searchStr);
        if (start == -1)
            return "";

        start += searchStr.length();
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
}
