# Long-Form Project Description

## Purpose

JavaQC is a Java-based quantum computing project that combines a circuit model, a local simulator, output rendering, and cloud-provider integration behind a small and readable API. The project exists to make quantum program structure visible. It is designed so that a circuit can be defined once, executed locally without secrets, rendered in several formats, and then adapted to cloud backends when needed.

The codebase is intentionally educational as well as practical. It is not only a runtime for circuits; it is also a reference implementation for how a simple quantum API can be layered in Java. The project shows how a program object moves through execution, how measurement state is preserved, how results are rendered, and how provider-specific details are isolated behind adapters.

## Big Idea

The central idea in JavaQC is separation of concerns.

The circuit model describes what should happen. The execution environment decides how it should happen. The printer decides how it should be shown. The cloud adapters decide how it should be translated for an external service. Secret loading is kept separate again so provider credentials do not leak into normal circuit code.

That division is what makes the repository easier to understand than a monolithic simulator. It also makes it safer to extend because a new provider can be added by implementing the adapter contract without rewriting the core model.

## What The Project Contains

At the highest level, the repository includes five major layers.

The first layer is the core quantum model in `org.redfx.strange`. This is where programs, steps, gates, qubits, and results are represented. The model is small on purpose. It forms the stable vocabulary used by the rest of the system.

The second layer is the local simulator in `org.redfx.strange.local`. This layer performs state-vector execution and provides a no-credential run path. For most contributors and users, this is the easiest way to see the repository working immediately.

The third layer is the rendering and serialization code in `org.redfx.strange.print`. This layer converts a circuit and its results into plain text, structured JSON, OpenQASM, or an ASCII circuit diagram. It is what makes the project useful in terminals, scripts, docs, and browser previews.

The fourth layer is the cloud adapter tree in `com.gluonhq.strange.cloudlink`. This is where IBM Quantum, Qiskit, Pasqal, D-Wave, and other provider-facing integrations are isolated behind a common contract.

The fifth layer is the runnable surface, including CLI demos, container entry points, and the small browser preview under `site/`.

## The Circuit Model

The project starts with `Program`, which represents a quantum circuit as an ordered set of steps. A program knows how many qubits it owns, what initial values those qubits should use, what steps have been added, and what result was produced after execution.

`Step` groups gates that should be applied together. This lets the circuit express simultaneous operations without flattening everything into one linear list of instructions. It is a useful abstraction because quantum execution frequently depends on which gates can happen in the same layer.

`Gate` is the unit of operation. Individual gate types such as Hadamard, CNOT, Swap, Toffoli, measurement, and the arithmetic or oracle-related gates each express a concrete transformation or observation.

`Qubit` and `Result` store the outcome. In a quantum model, the interesting part is not just the final measured bitstring. Intermediate probabilities, amplitudes, and measurement results matter too, so the result object keeps that information available for presentation and debugging.

## Why The Model Matters

The model is the stable public language of the repository. Everything else is built around it.

The simulator consumes a `Program`. The cloud adapters translate a `Program`. The printers render a `Program` and its `Result`. The tests validate behavior through the model. If the model stays coherent, the rest of the system remains understandable.

This is one of the reasons the repository is approachable for readers who are new to quantum computing. They can learn the structure from the code itself instead of from a hidden runtime or a large framework.

## Local Execution

The local simulator is the default execution path. It exists so that the repository is useful without any external services.

This matters for a few reasons. First, it gives every contributor a way to run the project right away. Second, it makes automated testing simpler because tests do not need live provider credentials. Third, it provides a clear baseline for understanding what the project is supposed to do before cloud-specific behavior is introduced.

The local execution engine walks through the steps in order, applies the gates, updates the state vector, and stores the result back on the program. That flow is easy to describe and easy to reason about, which is important for a teaching-oriented codebase.

## Rendering And Output

One of JavaQC’s strengths is that it can show the same circuit in multiple output forms.

Plain text output is the most direct format. It is suitable for terminal runs, CI logs, and quick inspection.

JSON output is useful for automation and browser-based views. It makes the program structure and result easier to feed into other tools.

ASCII circuit output is useful when a human wants to inspect the layout of the circuit without leaving the terminal.

OpenQASM export is important because it turns the Strange circuit model into a format understood by other ecosystems. This creates a bridge between the repository’s internal representation and broader quantum tooling.

The renderer layer is therefore not just cosmetic. It is part of the interoperability story.

## Cloud Integration

Cloud execution is handled through adapters. That is a deliberate design choice.

Rather than wiring the core model directly to provider APIs, the repository defines a provider contract and then creates provider-specific translation classes. This avoids polluting the core quantum model with HTTP logic, credentials handling, or SDK-specific code.

That pattern also makes it possible to support different providers without changing user-facing circuit code. A program can be created once and then sent to IBM Quantum, Qiskit, Pasqal, or another backend through the appropriate adapter.

The adapter layer is also where provider differences are normalized. Different services expose different formats, authentication models, and response payloads. The adapter is the place where those differences are absorbed so the rest of the system can continue speaking in Strange terms.

## Secret Handling

Cloud providers often require secrets, tokens, or workspace identifiers. JavaQC treats that as an opt-in concern.

The repository includes vault and environment-based secret loading so provider credentials do not need to live in source code. This is important for safety, but it is also important for usability. A repository that can run locally without secrets is much easier to try, test, and teach with.

The design encourages a simple rule: the default path should not require cloud configuration. Cloud configuration should only appear when the user intentionally chooses a provider that needs it.

## CLI And Demo Flow

The CLI demo entry point demonstrates the whole system in a small surface area.

It parses command-line arguments, builds an example circuit such as Bell, GHZ, Swap, or Toffoli, chooses a provider, executes the circuit, and prints the output in the selected format. Because the demo is compact, it acts as a practical tour of the project’s layers.

The demo is also useful as a regression anchor. If the demo continues to work, then the core model, simulator, printers, and selected providers are at least cooperating at a basic level.

## Repository Structure As A Learning Path

The repository is organized to support a sensible reading order.

A new reader can begin with `Program`, `Step`, and `Gate` to understand the model. From there they can move into the local simulator to see how execution works. After that they can inspect the printer layer to see how output is produced. Finally, they can read the adapters to understand how cloud execution is integrated.

That order mirrors the actual flow of data through the system. It reduces the mental overhead of reading the code because each layer builds on the previous one.

## Why The Project Is Useful

JavaQC has value in at least four contexts.

It is a learning project for quantum programming concepts because the execution path is small enough to follow.

It is a practical simulator for trying simple circuits without cloud dependencies.

It is an adapter example for showing how to isolate provider-specific code from a stable core model.

It is a documentation and rendering example for showing how the same underlying data can be exported in several forms.

## How The Pieces Fit Together

The normal path is simple.

1. A `Program` is created.
2. One or more `Step` instances are added.
3. Each step adds one or more `Gate` instances.
4. The selected execution environment runs the program.
5. A `Result` is returned.
6. `TextPrinter` or another renderer turns the result into a readable form.

That is the project in one sentence. Everything else is an implementation detail of one of those six stages.

## Extension Points

There are several natural places to extend the project.

New gates can be added when a circuit operation is missing.

New simulator behavior can be added when the execution engine needs richer support.

New printers can be added if another output format is needed.

New cloud adapters can be added if a provider should be supported.

New secret sources can be added if a deployment environment requires a different credential strategy.

The architecture is intentionally open to that kind of growth.

## Constraints And Design Rules

The most important design rule is that local execution should remain the default no-secrets path.

Another rule is that the core model should stay provider-agnostic. If a provider-specific change is needed, it belongs in the adapter layer rather than in the model.

Another rule is that output rendering should not depend on execution strategy. The same `Result` should be printable regardless of where it came from.

Those constraints keep the repository coherent as it grows.

## What A Reader Should Expect

A reader should expect a project that is simple in structure but broad in use.

It is not trying to hide quantum concepts behind a framework. Instead, it exposes them clearly. It is not trying to make every provider look identical by forcing everything into one lowest-common-denominator API. Instead, it normalizes only what the core model needs and leaves provider differences in the adapter layer.

That balance is what gives the repository its identity.

## Summary

JavaQC is a layered Java quantum computing project built around a clear circuit model, a local simulator, multiple output formats, and optional cloud adapters. It is designed to be educational, inspectable, and practical. A user can read the code, run the examples, inspect the outputs, and understand where to extend the system without first decoding a large framework.

If you want to understand the project deeply, start with the circuit model, then the local simulator, then the printers, and finally the provider adapters. That path reflects how the repository itself is organized.
