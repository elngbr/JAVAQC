package com.gluonhq.strange.cloudlink.adapters;

import com.gluonhq.strange.cloudlink.providers.QuantumCloudProvider;
import org.redfx.strange.Program;
import org.redfx.strange.Result;
import org.redfx.strange.Qubit;
import org.redfx.strange.Complex;
import org.redfx.strange.print.TextPrinter;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Pure Java IBM Quantum adapter using HTTP client.
 * Converts Program to OpenQASM, sends to IBM REST API, and returns results.
 */
public class IbmQuantumAdapter implements QuantumCloudProvider {

    private static final String IBM_API_URL = "https://api.quantum-computing.ibm.com/api";
    private final String token;
    private final String backend;
    private final HttpClient httpClient;

    public IbmQuantumAdapter(String token, String backend) {
        this.token = token != null ? token : System.getenv("IBM_QUANTUM_TOKEN");
        this.backend = backend != null ? backend : "ibmq_qasm_simulator";
        this.httpClient = HttpClient.newHttpClient();
    }

    public IbmQuantumAdapter(String token) {
        this(token, "ibmq_qasm_simulator");
    }

    @Override
    public Result submitProgram(Program program, int shots) throws Exception {
        Map<String, Integer> counts = getMeasurementCounts(program, shots);
        // Convert counts to Result
        int nq = program.getNumberQubits();
        int size = 1 << nq;
        Complex[] prob = new Complex[size];
        for (int i = 0; i < size; i++) {
            prob[i] = new Complex(0, 0);
        }
        int totalShots = counts.values().stream().mapToInt(Integer::intValue).sum();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            int idx = Integer.parseInt(e.getKey(), 2);
            double val = (double) e.getValue() / totalShots;
            prob[idx] = new Complex(Math.sqrt(val), 0);
        }
        return new Result(new Qubit[nq], prob);
    }

    @Override
    public Map<String, Integer> getMeasurementCounts(Program program, int shots) throws Exception {
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("IBM token not set. Use IBM_QUANTUM_TOKEN env var or constructor.");
        }
        String qasm = TextPrinter.toOpenQasm(program);
        JSONObject request = new JSONObject();
        request.put("qasm", qasm);
        request.put("shots", shots);
        request.put("backend", backend);
        String body = request.toString();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(new URI(IBM_API_URL + "/submit"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200 && resp.statusCode() != 201) {
            throw new RuntimeException("IBM API error: " + resp.statusCode() + " " + resp.body());
        }
        JSONObject respJson = new JSONObject(resp.body());
        Map<String, Integer> counts = new HashMap<>();
        if (respJson.has("counts")) {
            JSONObject c = respJson.getJSONObject("counts");
            for (String key : c.keySet()) {
                counts.put(key, c.getInt(key));
            }
        }
        return counts;
    }

    @Override
    public String getProviderName() {
        return "IBM Quantum";
    }

    @Override
    public boolean isAvailable() {
        return token != null && !token.isEmpty();
    }
}
