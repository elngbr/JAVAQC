import os, sys
token = args.token or os.environ.get("IBM_QUANTUM_TOKEN")
if not token:
    print("Provide IBM token via --token or IBM_QUANTUM_TOKEN env var", file=sys.stderr)
    sys.exit(2)    String token = System.getenv("IBM_QUANTUM_TOKEN");
    if (token == null || token.isEmpty()) {
        throw new IllegalStateException("IBM_QUANTUM_TOKEN not set");
    }