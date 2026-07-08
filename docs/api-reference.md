# API Reference

This is a compact map of the main classes and methods you will use when reading or extending the codebase.

## Core Model

### Program

File: `src/main/java/org/redfx/strange/Program.java`

Responsibilities:

- holds the number of qubits
- stores ordered `Step` instances
- stores execution results
- validates measurement safety when adding steps

Key methods:

- `Program(int nQubits, Step... moreSteps)`
- `addStep(Step step)`
- `addSteps(Step... moreSteps)`
- `getSteps()`
- `getNumberQubits()`
- `getResult()`
- `setResult(Result r)`

### Step

File: `src/main/java/org/redfx/strange/Step.java`

Responsibilities:

- groups gates that execute together
- stores gate order inside the step

### Gate

File: `src/main/java/org/redfx/strange/Gate.java`

Responsibilities:

- defines the common contract for operations on qubits
- exposes affected qubit indexes and gate metadata

### QuantumExecutionEnvironment

File: `src/main/java/org/redfx/strange/QuantumExecutionEnvironment.java`

Responsibilities:

- defines synchronous and callback-based execution
- provides the contract implemented by local and cloud backends

### Qubit and Result

Files:

- `src/main/java/org/redfx/strange/Qubit.java`
- `src/main/java/org/redfx/strange/Result.java`

Responsibilities:

- `Qubit` stores per-qubit state and measurement probability
- `Result` stores the full execution outcome

## Local Execution

### SimpleQuantumExecutionEnvironment

File: `src/main/java/org/redfx/strange/local/SimpleQuantumExecutionEnvironment.java`

Responsibilities:

- decomposes complex steps when needed
- applies each step to the state vector
- computes qubit probabilities
- stores the final `Result` on the `Program`

## Printing and Serialization

### TextPrinter

File: `src/main/java/org/redfx/strange/print/TextPrinter.java`

Key methods:

- `toPlain(Program, Result)`
- `toJson(Program, Result)`
- `toOpenQasm(Program)`
- `toProbabilities(Result)`
- `toStructuredJson(Program)`
- `toCircuit(Program)`

## CLI Entry Points

### TextDemo

File: `src/main/java/org/redfx/strange/demo/TextDemo.java`

Responsibilities:

- selects local or cloud execution
- builds example programs like Bell, GHZ, Swap, and Toffoli
- prints results in plain text, JSON, or circuit form
- can export structured JSON for browser rendering

### Demo

File: `src/main/java/org/redfx/strange/demo/Demo.java`

Responsibilities:

- legacy demo entry point
- exercises the simulator and matrix utilities

## Cloud Layer

### QuantumCloudProvider

File: `src/main/java/com/gluonhq/strange/cloudlink/providers/QuantumCloudProvider.java`

Responsibilities:

- defines the adapter contract for cloud execution

### Adapters

Files under `src/main/java/com/gluonhq/strange/cloudlink/adapters/`

Responsibilities:

- translate Strange programs into provider-specific formats
- submit the program or request to the provider
- convert provider responses back into `Result`

### Vaults

Files under `src/main/java/com/gluonhq/strange/cloudlink/vault/`

Responsibilities:

- abstract credential loading
- support environment variables, Azure Key Vault, and HashiCorp Vault

## Testing

Existing tests live under `src/test/java/org/redfx/strange/` and focus on core model and local simulator behavior.
