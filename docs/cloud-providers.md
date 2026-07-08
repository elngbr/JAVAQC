# Cloud Providers

JavaQC uses a common adapter contract for cloud execution:

- `QuantumCloudProvider.submitProgram(Program, int)`
- `QuantumCloudProvider.getMeasurementCounts(Program, int)`
- `QuantumCloudProvider.getProviderName()`
- `QuantumCloudProvider.isAvailable()`

## Provider Map

| Provider               | Adapter               | Transport / Translation           | Status         |
| ---------------------- | --------------------- | --------------------------------- | -------------- |
| IBM Quantum            | `IbmQuantumAdapter`   | Java `HttpClient` plus OpenQASM   | implemented    |
| IBM Quantum via Qiskit | `QiskitAdapter`       | Python bridge via `ibm_submit.py` | implemented    |
| Pasqal                 | `PasqualAdapter`      | HTTP plus Pulser-style payload    | implemented    |
| D-Wave                 | `DWaveAdapter`        | QUBO / Ising path planned         | stub / partial |
| QMware                 | `QMwareAdapter`       | provider-specific translation     | present        |
| Xanadu                 | `XanaduAdapter`       | provider-specific translation     | present        |
| Google Cirq            | `GoogleCirqAdapter`   | provider-specific translation     | present        |
| Azure Quantum          | `AzureQuantumAdapter` | provider-specific translation     | present        |

## Execution Path

1. `TextDemo` selects a provider from the CLI.
2. The selected adapter converts `Program` into the provider format.
3. The provider executes the circuit or returns sampled counts.
4. The adapter converts the provider response back to `Result`.
5. `TextPrinter` formats the output for the console or JSON.

## IBM Quantum

There are two IBM paths.

### Pure Java

`IbmQuantumAdapter` converts the program to OpenQASM and submits it with `HttpClient`.

Use this when you want a minimal Java-only integration.

### Qiskit Bridge

`QiskitAdapter` writes OpenQASM to a temporary file and calls `ibm_submit.py` with Python.

Use this when you want a Python-based bridge and do not mind the extra runtime dependency.

## Pasqal

Pasqal uses `PasqualAdapter` plus the vault layer.

- secrets can come from Azure Key Vault
- secrets can come from HashiCorp Vault
- environment variables are the fallback for development

See [vault and secrets](vault-and-secrets.md) and [Pasqal integration](pasqal-integration.md).

## D-Wave

`DWaveAdapter` is the annealing-oriented cloud entry point.

What it already does:

- reads `DWAVE_API_KEY` and `DWAVE_SOLVER_ID` from the environment
- checks whether the adapter is available before execution
- preserves the common `QuantumCloudProvider` contract
- returns a placeholder `Result` shell so the class compiles as a scaffold

What it is intended to do later:

- convert a Strange `Program` into QUBO or Ising form
- submit that optimization problem to the Ocean API
- collect samples from the annealer or hybrid solver
- map those samples back into Strange-style counts and results

That makes D-Wave different from the gate-based providers: it is not a direct circuit executor, but an optimization translation layer.

## Important Behavior

Cloud execution is optional. A clean clone can always run the local simulator without cloud credentials.
