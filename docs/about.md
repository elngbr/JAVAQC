# Project Overview

## Introduction

JavaQC is a Java quantum computing API and simulator built on the Strange circuit model. It is designed to be didactic first, while still being useful for local simulation, console output, and cloud provider integration.

JavaQC is a quantum computing playground and API for Java. It preserves the Strange circuit model and adds container-friendly output, cloud adapters, and vault-backed secret handling.

## What This Project Tries To Do

- keep the Strange execution model intact
- provide a local simulator for zero-secret runs
- expose cloud provider integrations through adapters
- print circuits and results in text, JSON, and OpenQASM forms
- support container and CLI workflows

## Main Building Blocks

- `Program`, `Step`, `Gate`, `Qubit`, and `Result` define the circuit model
- `QuantumExecutionEnvironment` runs the program
- `SimpleQuantumExecutionEnvironment` executes locally
- `QuantumCloudProvider` defines the cloud adapter contract
- `TextPrinter` formats output for the terminal and for structured export

## Repository Layout

- `src/main/java/org/redfx/strange/` contains the core API and simulator code
- `src/main/java/com/gluonhq/strange/cloudlink/` contains cloud integrations and vault support
- `src/main/java/org/redfx/strange/print/` contains serializers and circuit rendering
- `src/main/java/org/redfx/strange/demo/` contains runnable demos
- `site/` contains the lightweight browser preview
- `docs/` contains the written documentation set

## Read Next

- [Architecture](architecture.md)
- [Getting started](getting-started.md)
- [Cloud providers](cloud-providers.md)
- [Vaults and secrets](vault-and-secrets.md)
- [API reference](api-reference.md)
- [Pasqal integration](pasqal-integration.md)
