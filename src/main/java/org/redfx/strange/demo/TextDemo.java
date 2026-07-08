package org.redfx.strange.demo;

import org.redfx.strange.*;
import org.redfx.strange.gate.*;
import org.redfx.strange.print.TextPrinter;
import org.redfx.strange.local.SimpleQuantumExecutionEnvironment;
import com.gluonhq.strange.cloudlink.adapters.IbmQuantumAdapter;
import com.gluonhq.strange.cloudlink.adapters.PasqualAdapter;
import com.gluonhq.strange.cloudlink.adapters.QiskitAdapter;
import com.gluonhq.strange.cloudlink.providers.QuantumCloudProvider;

/**
 * CLI demo that prints program and result in plain or JSON and can submit to
 * cloud providers like IBM (pure Java or Qiskit), Pasqal, etc.
 */
public class TextDemo {

    public static void main(String[] args) throws Exception {
        String format = "plain";
        String provider = "local";
        String backend = "ibmq_qasm_simulator";
        int shots = 1024;
        String example = "bell";
        String exportJson = null;
        // Parse the demo flags manually so the entry point stays dependency-free and
        // easy to run
        // from a plain java command line.
        for (int i = 0; i < args.length; i++) {
            if ("--format".equals(args[i]) && i + 1 < args.length)
                format = args[++i];
            if ("--provider".equals(args[i]) && i + 1 < args.length)
                provider = args[++i];
            if ("--backend".equals(args[i]) && i + 1 < args.length)
                backend = args[++i];
            if ("--shots".equals(args[i]) && i + 1 < args.length)
                shots = Integer.parseInt(args[++i]);
            if ("--example".equals(args[i]) && i + 1 < args.length)
                example = args[++i];
            if ("--export-json".equals(args[i]) && i + 1 < args.length)
                exportJson = args[++i];
        }

        Program p;
        // Build a small example circuit so each provider path can be exercised with the
        // same shape.
        switch (example.toLowerCase()) {
            case "ghz":
                p = new Program(3);
                Step g1 = new Step();
                g1.addGate(new Hadamard(0));
                p.addStep(g1);
                Step g2 = new Step();
                g2.addGate(new Cnot(0, 1));
                p.addStep(g2);
                Step g3 = new Step();
                g3.addGate(new Cnot(0, 2));
                p.addStep(g3);
                Step gm = new Step();
                gm.addGates(new Measurement(0), new Measurement(1), new Measurement(2));
                p.addStep(gm);
                break;
            case "swap":
                p = new Program(2);
                Step x1 = new Step();
                x1.addGate(new X(0));
                p.addStep(x1);
                Step sw = new Step();
                sw.addGate(new Swap(0, 1));
                p.addStep(sw);
                Step sm = new Step();
                sm.addGates(new Measurement(0), new Measurement(1));
                p.addStep(sm);
                break;
            case "toffoli":
                p = new Program(3);
                Step t1 = new Step();
                t1.addGate(new Hadamard(2));
                p.addStep(t1);
                Step t2 = new Step();
                t2.addGate(new Toffoli(0, 1, 2));
                p.addStep(t2);
                Step tm = new Step();
                tm.addGates(new Measurement(0), new Measurement(1), new Measurement(2));
                p.addStep(tm);
                break;
            default:
                // bell
                p = new Program(2);
                Step s1 = new Step();
                s1.addGate(new Hadamard(0));
                p.addStep(s1);
                Step s2 = new Step();
                s2.addGate(new Cnot(0, 1));
                p.addStep(s2);
                Step s3 = new Step();
                s3.addGates(new Measurement(0), new Measurement(1));
                p.addStep(s3);
                break;
        }

        // If exportJson is requested, write structured JSON and exit before any
        // execution happens.
        if (exportJson != null) {
            try {
                org.json.JSONObject obj = TextPrinter.toStructuredJson(p);
                java.nio.file.Path path = java.nio.file.Paths.get(exportJson);
                java.nio.file.Files
                        .createDirectories(path.getParent() == null ? java.nio.file.Paths.get(".") : path.getParent());
                java.nio.file.Files.writeString(path, obj.toString(2));
                System.out.println("Wrote " + exportJson);
                return;
            } catch (Exception ex) {
                System.err.println("Failed to write export json: " + ex.getMessage());
                System.exit(1);
            }
        }

        // Provider-specific branches keep the local simulator as the default path and
        // make cloud
        // execution opt-in only.
        if ("ibm".equalsIgnoreCase(provider)) {
            QuantumCloudProvider cloudProvider = new IbmQuantumAdapter(null, backend);
            if ("circuit".equalsIgnoreCase(format)) {
                System.out.println(TextPrinter.toCircuit(p));
            }
            if (!cloudProvider.isAvailable()) {
                System.err.println("Error: IBM token not available. Set IBM_QUANTUM_TOKEN env var.");
                System.exit(1);
            }
            Result res = cloudProvider.submitProgram(p, shots);
            if ("json".equalsIgnoreCase(format)) {
                System.out.println(TextPrinter.toJson(p, res));
            } else if ("circuit".equalsIgnoreCase(format)) {
                // circuit already printed before run; show probabilities only
                System.out.println(TextPrinter.toProbabilities(res));
            } else {
                System.out.println(TextPrinter.toPlain(p, res));
            }
            return;
        }

        if ("qiskit".equalsIgnoreCase(provider)) {
            QuantumCloudProvider cloudProvider = new QiskitAdapter(null, backend, null);
            if ("circuit".equalsIgnoreCase(format)) {
                System.out.println(TextPrinter.toCircuit(p));
            }
            if (!cloudProvider.isAvailable()) {
                System.err.println("Error: IBM token not available or ibm_submit.py script not found.");
                System.err.println("Set IBM_QUANTUM_TOKEN env var and ensure ibm_submit.py is in project root.");
                System.exit(1);
            }
            Result res = cloudProvider.submitProgram(p, shots);
            if ("json".equalsIgnoreCase(format)) {
                System.out.println(TextPrinter.toJson(p, res));
            } else if ("circuit".equalsIgnoreCase(format)) {
                System.out.println(TextPrinter.toProbabilities(res));
            } else {
                System.out.println(TextPrinter.toPlain(p, res));
            }
            return;
        }

        if ("pasqual".equalsIgnoreCase(provider)) {
            QuantumCloudProvider cloudProvider = new PasqualAdapter();
            System.out.println(TextPrinter.toCircuit(p));
            if (!cloudProvider.isAvailable()) {
                System.err.println(
                        "Error: Pasqal credentials not available. Set PASQAL_CLIENT_ID, PASQAL_CLIENT_SECRET, PASQAL_PROJECT_ID, and PASQAL_EMAIL env vars.");
                System.exit(1);
            }
            Result res = cloudProvider.submitProgram(p, shots);
            if ("json".equalsIgnoreCase(format)) {
                System.out.println(TextPrinter.toJson(p, res));
            } else if ("circuit".equalsIgnoreCase(format)) {
                System.out.println(TextPrinter.toProbabilities(res));
            } else {
                System.out.println(TextPrinter.toPlain(p, res));
            }
            return;
        }

        // Local execution is the fallback so the demo works without credentials or
        // external services.
        SimpleQuantumExecutionEnvironment sqee = new SimpleQuantumExecutionEnvironment();
        if ("circuit".equalsIgnoreCase(format)) {
            System.out.println(TextPrinter.toCircuit(p));
        }
        Result res = sqee.runProgram(p);

        if ("json".equalsIgnoreCase(format)) {
            System.out.println(TextPrinter.toJson(p, res));
        } else if ("circuit".equalsIgnoreCase(format)) {
            System.out.println(TextPrinter.toProbabilities(res));
        } else {
            System.out.println(TextPrinter.toPlain(p, res));
        }
    }
}
