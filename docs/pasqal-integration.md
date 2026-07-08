# Pasqal Integration

## What We Had Before

Before this work, the repository already had:

- a Pasqal adapter stub in [src/main/java/com/gluonhq/strange/cloudlink/adapters/PasqualAdapter.java](../src/main/java/com/gluonhq/strange/cloudlink/adapters/PasqualAdapter.java)
- a simple `site/` folder with a static viewer and a minimal backend in [site/render.html](../site/render.html) and [site/server.py](../site/server.py)
- a sample circuit JSON file in [site/program.json](../site/program.json)

## What We Added

### Pasqal integration

The Pasqal adapter now supports:

- loading credentials from a vault or environment variables
- using the hardcoded development email `elenaeft07@gmail.com`
- using the project ID `591eb05c-88c2-4ca5-b228-5fd91e64855f`
- authenticating against Pasqal APIs through an OAuth-style token flow
- submitting circuit batches and polling for results

The supporting vault layer includes:

- `SecretVault`
- `HashiCorpVaultClient`
- `AzureKeyVaultClient`
- `EnvironmentVariableVault`
- `VaultFactory`

## How It Works

### 1. Credentials are loaded

The Pasqal setup script [setup_pasqal.sh](../setup_pasqal.sh) sets:

- `PASQAL_EMAIL=elenaeft07@gmail.com`
- `PASQAL_PROJECT_ID=591eb05c-88c2-4ca5-b228-5fd91e64855f`

You still need real credentials for actual API calls:

- `PASQAL_CLIENT_ID`
- `PASQAL_CLIENT_SECRET`

### 1a. The UI is credential-gated

The browser UI now opens with a Pasqal access panel. The panel asks for:

- email
- project ID

The current repository is configured around:

- `elenaeft07@gmail.com`
- `591eb05c-88c2-4ca5-b228-5fd91e64855f`

When those values are entered, the studio unlocks and the same values are sent with the run request.

### 2. The adapter builds a request

`PasqualAdapter` reads credentials, gets an access token, converts the Strange program to a Pulser-style payload, and submits a batch to Pasqal.

### 3. The backend runs the circuit

The backend forwards the Pasqal email and project ID to the Java process through environment variables.

`TextDemo` now has a Pasqal provider branch, so the UI can run a circuit through the same Java execution path as the other cloud providers.

The backend can also export the built-in Strange example program to `site/program.json` and return it for rendering or inspection.

## Local Run Steps

```bash
cd /Users/elenaeftimie/Desktop/javaqc
mvn package -DskipTests
cd site
PORT=8001 python3 server.py
```

Then you can inspect the generated JSON in `site/program.json` or use the site files as a lightweight renderer.

## UI Flow

1. Open the Circuit Studio page.
2. Enter the Pasqal email and project ID in the access panel.
3. Click `Unlock Pasqal Studio`.
4. Choose `Pasqual` as the provider.
5. Edit the JSON or use the gate buttons.
6. Click `Run / Preview` to send the same credentials and circuit to the backend.

## Notes

- The current preview path is local-first; real Pasqal execution still depends on valid credentials and platform access.
- The default sample circuit is the Bell state example in `site/program.json`.
