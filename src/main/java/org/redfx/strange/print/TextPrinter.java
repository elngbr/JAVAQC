package org.redfx.strange.print;

import org.redfx.strange.Program;
import org.redfx.strange.Result;
import org.redfx.strange.Step;
import org.redfx.strange.Gate;
import org.redfx.strange.gate.*;

import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * minimal text/JSON/OpenQASM printer for Program and Result.
 */
public class TextPrinter {

    public static String toPlain(Program p, Result r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Program with ").append(p.getNumberQubits()).append(" qubits\n");
        sb.append("Steps:\n");
        for (Step s : p.getSteps()) {
            sb.append(" - ");
            sb.append(s.getGates().toString()).append("\n");
        }
        if (r != null && r.getProbability() != null) {
            sb.append("Probabilities:\n");
            for (int i = 0; i < r.getProbability().length; i++) {
                sb.append(String.format("  %d: %.6f\n", i, r.getProbability()[i].abssqr()));
            }
        }
        return sb.toString();
    }

    public static String toJson(Program p, Result r) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"nqubits\":").append(p.getNumberQubits()).append(",");
        sb.append("\"steps\":");
        sb.append("[");
        boolean first = true;
        for (Step s : p.getSteps()) {
            if (!first)
                sb.append(",");
            first = false;
            sb.append("\"").append(escape(s.getGates().toString())).append("\"");
        }
        sb.append("]");
        if (r != null && r.getProbability() != null) {
            sb.append(",\"probabilities\":{");
            for (int i = 0; i < r.getProbability().length; i++) {
                if (i > 0)
                    sb.append(",");
                sb.append("\"").append(i).append("\":").append(String.format("%.6f", r.getProbability()[i].abssqr()));
            }
            sb.append("}");
        }
        sb.append("}");
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    public static String toOpenQasm(Program p) {
        int nq = p.getNumberQubits();
        StringBuilder sb = new StringBuilder();
        sb.append("OPENQASM 2.0;\ninclude \"qelib1.inc\";\n");
        sb.append("qreg q[").append(nq).append("];\n");
        sb.append("creg c[").append(nq).append("];\n");
        for (Step s : p.getSteps()) {
            for (Gate g : s.getGates()) {
                if (g instanceof Hadamard) {
                    int q = g.getMainQubitIndex();
                    sb.append("h q[").append(q).append("];\n");
                } else if (g instanceof X) {
                    int q = g.getMainQubitIndex();
                    sb.append("x q[").append(q).append("];\n");
                } else if (g instanceof Y) {
                    int q = g.getMainQubitIndex();
                    sb.append("y q[").append(q).append("];\n");
                } else if (g instanceof Z) {
                    int q = g.getMainQubitIndex();
                    sb.append("z q[").append(q).append("];\n");
                } else if (g instanceof Cnot) {
                    Cnot c = (Cnot) g;
                    sb.append("cx q[").append(c.getMainQubitIndex()).append("],q[").append(c.getSecondQubitIndex())
                            .append("];\n");
                } else if (g instanceof Measurement) {
                    int q = g.getMainQubitIndex();
                    sb.append("measure q[").append(q).append("] -> c[").append(q).append("];\n");
                } else {
                    // unsupported gate -> comment
                    sb.append("// unsupported: ").append(g.getClass().getSimpleName()).append("\n");
                }
            }
        }
        return sb.toString();
    }

    /**
     * return only the probabilities block for a result.
     */
    public static String toProbabilities(Result r) {
        if (r == null || r.getProbability() == null)
            return "No result available";
        StringBuilder sb = new StringBuilder();
        sb.append("Probabilities:\n");
        for (int i = 0; i < r.getProbability().length; i++) {
            sb.append(String.format("  %d: %.6f\n", i, r.getProbability()[i].abssqr()));
        }
        return sb.toString();
    }

    /**
     * Serialize program to a structured JSON object suitable for browser rendering.
     * Format: { nqubits: N, steps: [ { gates: [ { type: "H", targets: [0], caption:
     * "Hadamard" }, ... ] }, ... ] }
     */
    public static JSONObject toStructuredJson(Program p) {
        JSONObject root = new JSONObject();
        root.put("nqubits", p.getNumberQubits());
        JSONArray stepsArr = new JSONArray();
        for (Step s : p.getSteps()) {
            JSONObject stepObj = new JSONObject();
            JSONArray gatesArr = new JSONArray();
            for (Gate g : s.getGates()) {
                JSONObject gateObj = new JSONObject();
                String type = g.getClass().getSimpleName();
                gateObj.put("type", type);
                gateObj.put("caption", g.getCaption());
                JSONArray targets = new JSONArray();
                List<Integer> affected = g.getAffectedQubitIndexes();
                if (affected != null) {
                    for (Integer qi : affected)
                        targets.put(qi);
                }
                gateObj.put("targets", targets);
                gatesArr.put(gateObj);
            }
            stepObj.put("gates", gatesArr);
            stepsArr.put(stepObj);
        }
        root.put("steps", stepsArr);
        return root;
    }

    /**
     * Produce a simple ASCII circuit diagram similar to Qiskit's text drawer.
     * Each step is rendered as a fixed-width column. Supports single-qubit gates,
     * two-qubit controlled gates (Cnot) and measurements.
     */
    public static String toCircuit(Program p) {
        int nq = p.getNumberQubits();
        List<Step> steps = p.getSteps();
        int cellWidth = 7; // characters per cell; wide enough for connected box gates

        String[] topRows = new String[nq];
        String[] midRows = new String[nq];
        String[] botRows = new String[nq];
        String[] linkRows = new String[Math.max(0, nq - 1)];
        for (int i = 0; i < nq; i++) {
            topRows[i] = "";
            midRows[i] = "";
            botRows[i] = "";
        }
        for (int i = 0; i < linkRows.length; i++) {
            linkRows[i] = "";
        }

        for (Step s : steps) {
            String[] topCell = new String[nq];
            String[] midCell = new String[nq];
            String[] botCell = new String[nq];
            String[] linkCell = new String[Math.max(0, nq - 1)];
            for (int i = 0; i < nq; i++) {
                topCell[i] = "       ";
                midCell[i] = "───────";
                botCell[i] = "       ";
            }
            for (int i = 0; i < linkCell.length; i++) {
                linkCell[i] = "       ";
            }

            for (Gate g : s.getGates()) {
                java.util.List<Integer> affected = g.getAffectedQubitIndexes();
                if (affected == null || affected.isEmpty())
                    continue;
                if (affected.size() == 1) {
                    int q = affected.get(0);
                    String cap = shortCap(g.getCaption());
                    topCell[q] = " ┌───┐ ";
                    midCell[q] = "─┤ " + padCap(cap) + " ├─";
                    botCell[q] = " └───┘ ";
                } else if (affected.size() == 2) {
                    int a = affected.get(0);
                    int b = affected.get(1);
                    int min = Math.min(a, b);
                    int max = Math.max(a, b);
                    midCell[min] = "───●───";
                    midCell[max] = "───X───";
                    for (int j = min + 1; j < max; j++) {
                        midCell[j] = "───│───";
                    }
                    for (int j = min; j < max; j++) {
                        linkCell[j] = "   │   ";
                    }
                } else if (g instanceof Toffoli && affected.size() == 3) {
                    int c1 = affected.get(0);
                    int c2 = affected.get(1);
                    int tgt = affected.get(2);
                    int min = Math.min(c1, Math.min(c2, tgt));
                    int max = Math.max(c1, Math.max(c2, tgt));
                    for (int j = min; j <= max; j++) {
                        midCell[j] = "───│───";
                    }
                    midCell[c1] = "───●───";
                    midCell[c2] = "───●───";
                    midCell[tgt] = "───X───";
                    for (int j = min; j < max; j++) {
                        linkCell[j] = "   │   ";
                    }
                } else {
                    for (int q : affected) {
                        midCell[q] = "───*───";
                    }
                    int min = affected.get(0);
                    int max = affected.get(0);
                    for (int q : affected) {
                        min = Math.min(min, q);
                        max = Math.max(max, q);
                    }
                    for (int j = min; j < max; j++) {
                        linkCell[j] = "   │   ";
                    }
                }
            }

            for (int i = 0; i < nq; i++) {
                topRows[i] += topCell[i];
                midRows[i] += midCell[i];
                botRows[i] += botCell[i];
            }
            for (int i = 0; i < linkRows.length; i++) {
                linkRows[i] += linkCell[i];
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nq; i++) {
            sb.append("q_").append(i).append(": ").append(rtrim(topRows[i])).append('\n');
            sb.append("     ").append(rtrim(midRows[i])).append('\n');
            sb.append("     ").append(rtrim(botRows[i])).append('\n');
            if (i < nq - 1) {
                sb.append("     ").append(rtrim(linkRows[i])).append('\n');
            }
        }

        int totalWidth = cellWidth * Math.max(1, steps.size());
        String eq = repeat('═', totalWidth);
        sb.append("c_0: ").append(eq).append('\n');
        sb.append("     ").append(repeat(' ', Math.max(0, totalWidth - 1))).append('\n');

        return sb.toString();
    }

    private static String shortCap(String caption) {
        if (caption == null || caption.isEmpty())
            return "?";
        // common captions: Hadamard -> H, X/Y/Z -> X/Y/Z, Measurement -> M
        caption = caption.trim();
        if (caption.equalsIgnoreCase("Hadamard"))
            return "H";
        if (caption.equalsIgnoreCase("Measurement"))
            return "M";
        if (caption.length() == 1)
            return caption.toUpperCase();
        return caption.substring(0, 1).toUpperCase();
    }

    private static String padCap(String s) {
        if (s == null)
            s = "?";
        if (s.length() >= 1)
            return s.substring(0, 1);
        return " ";
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++)
            sb.append(c);
        return sb.toString();
    }

    private static String rtrim(String s) {
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == ' ') {
            end--;
        }
        return s.substring(0, end);
    }
}
