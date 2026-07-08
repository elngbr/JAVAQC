package com.gluonhq.strange.cloudlink.adapters;

import com.gluonhq.strange.cloudlink.providers.QuantumCloudProvider;
import org.redfx.strange.Program;
import org.redfx.strange.Result;
import org.redfx.strange.print.TextPrinter;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Adapter for IBM Quantum using Qiskit Python library.
 * This adapter wraps the ibm_submit.py Python script to submit OpenQASM
 * circuits.
 * 
 * Requires:
 * - Python 3.9+
 * - qiskit and qiskit-ibm-runtime installed
 * - IBM_QUANTUM_TOKEN environment variable set
 * - ibm_submit.py script in project root
 */
public class QiskitAdapter implements QuantumCloudProvider {
    private String token;
    private String backend;
    private String scriptPath;

    public QiskitAdapter() {
        this(System.getenv("IBM_QUANTUM_TOKEN"), "ibmq_qasm_simulator", "./ibm_submit.py");
    }

    public QiskitAdapter(String token, String backend, String scriptPath) {
        this.token = token;
        this.backend = backend != null ? backend : "ibmq_qasm_simulator";
        this.scriptPath = scriptPath != null ? scriptPath : "./ibm_submit.py";
    }

    @Override
    public Result submitProgram(Program program, int shots) throws Exception {
        Map<String, Integer> counts = getMeasurementCounts(program, shots);
        return convertCountsToResult(program, counts);
    }

    @Override
    public Map<String, Integer> getMeasurementCounts(Program program, int shots) throws Exception {
        if (!isAvailable()) {
            throw new IllegalStateException("IBM Quantum token not configured or Qiskit script not found");
        }

        // Convert program to OpenQASM
        String qasm = TextPrinter.toOpenQasm(program);

        // Write QASM to temporary file
        Path tmpQasm = Files.createTempFile("circuit_", ".qasm");
        Files.write(tmpQasm, qasm.getBytes());

        try {
            // Call Python ibm_submit.py script
            ProcessBuilder pb = new ProcessBuilder(
                    "python3", scriptPath,
                    "--token", token,
                    "--backend", backend,
                    "--shots", String.valueOf(shots),
                    "--qasm", tmpQasm.toString());

            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Capture output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            int exitCode = process.waitFor();
            String jsonOutput = output.toString().trim();

            if (exitCode != 0) {
                throw new RuntimeException("Python script failed: " + jsonOutput);
            }

            // Parse JSON response
            JSONObject json = new JSONObject(jsonOutput);
            if (json.has("error")) {
                throw new RuntimeException("Qiskit error: " + json.getString("error"));
            }

            // Extract measurement counts
            JSONObject countsJson = json.getJSONObject("counts");
            Map<String, Integer> counts = new HashMap<>();
            for (String key : countsJson.keySet()) {
                counts.put(key, countsJson.getInt(key));
            }
            return counts;

        } finally {
            // Clean up temporary file
            Files.deleteIfExists(tmpQasm);
        }
    }

    @Override
    public String getProviderName() {
        return "IBM Quantum (Qiskit)";
    }

    @Override
    public boolean isAvailable() {
        if (token == null || token.isEmpty()) {
            return false;
        }
        // Check if script exists
        File script = new File(scriptPath);
        return script.exists() && script.isFile();
    }

    private Result convertCountsToResult(Program program, Map<String, Integer> counts) {
        int nqubits = program.getNumberQubits();
        return new Result(nqubits, 0);
    }
}
