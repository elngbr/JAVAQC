#!/usr/bin/env python3
"""
ibm_submit.py
Simple helper to submit an OpenQASM file to IBM Quantum using qiskit.
Reads token from --token or IBM_QUANTUM_TOKEN env var.
Prints a JSON object with counts to stdout.
"""
import argparse
import json
import os
import sys
from qiskit import QuantumCircuit, transpile
from qiskit import IBMQ


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--token", help="IBM token (optional: will use IBM_QUANTUM_TOKEN env var)")
    parser.add_argument("--backend", default="ibmq_qasm_simulator")
    parser.add_argument("--qasm", required=True)
    parser.add_argument("--shots", type=int, default=1024)
    args = parser.parse_args()

    token = args.token or os.environ.get("IBM_QUANTUM_TOKEN")
    if not token:
        print("Error: IBM Quantum token not provided (use --token or set IBM_QUANTUM_TOKEN)", file=sys.stderr)
        sys.exit(2)

    try:
        IBMQ.enable_account(token)
    except Exception:
        # enable_account may raise if already enabled - ignore
        pass

    providers = IBMQ.providers()
    if len(providers) == 0:
        print(json.dumps({"error": "no providers available"}))
        sys.exit(1)
    provider = providers[0]

    try:
        backend = provider.get_backend(args.backend)
    except Exception as e:
        print(json.dumps({"error": f"backend error: {e}"}))
        sys.exit(1)

    try:
        qc = QuantumCircuit.from_qasm_file(args.qasm)
    except Exception as e:
        print(json.dumps({"error": f"cannot parse qasm: {e}"}))
        sys.exit(1)

    tqc = transpile(qc, backend)
    job = backend.run(tqc, shots=args.shots)
    res = job.result()
    counts = res.get_counts()
    print(json.dumps({"counts": counts}))


if __name__ == '__main__':
    main()
