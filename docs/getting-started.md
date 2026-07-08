# Getting Started

This repository can be used in three main ways:

- local simulation with no external credentials
- cloud execution through provider adapters
- containerized CLI execution with plain text or JSON output

## Prerequisites

- Java 25 for the Maven build in this repository
- Maven 3.9+ for the main build path
- Optional: Docker or Podman for container runs
- Optional: Python 3.9+ for the Qiskit bridge

## Main Build

The primary build is Maven-based.

```bash
mvn test
mvn -DskipTests package
```

The packaged jar is configured to use `org.redfx.strange.demo.TextDemo` as the executable entry point.

## Local Run

Run the default simulator path:

```bash
java -jar target/javaqc-1.0.0-SNAPSHOT-jar-with-dependencies.jar --provider local --format plain
```

For a direct classpath run:

```bash
java -cp target/classes org.redfx.strange.demo.TextDemo --provider local --format circuit
```

## CLI Modes

`TextDemo` accepts:

- `--provider local|ibm|qiskit|pasqal|qmware|dwave|xanadu|google|azure`
- `--format plain|json|circuit`
- `--backend <name>` for IBM/Qiskit
- `--shots <n>` for provider sampling
- `--example bell|ghz|swap|toffoli`
- `--export-json <path>` to write structured circuit JSON

## Ant Build

A minimal Ant build is available for compatibility or inspection.

```bash
ant compile
ant jar
ant run
```

## Legacy Gradle Tree

There is also a separate Gradle-based tree under `gradlebuild/`. It reflects an older GluonHQ-oriented build and is best treated as a secondary build path unless you are working on that subproject specifically.

## Container Run

The repository includes Docker and Podman support.

```bash
docker build -t javaqc .
docker run --rm javaqc --format plain
```

or

```bash
podman build -t javaqc .
podman run --rm javaqc --format plain
```

## Recommended First Run

1. Build with Maven.
2. Run `TextDemo` with the local provider.
3. Enable one cloud provider only after the local path works.
4. Add secrets through a vault or environment configuration if you need cloud execution.
