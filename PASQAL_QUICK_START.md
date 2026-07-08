# Pasqal Quick Start Guide (Email-Based)

## Setup with Hardcoded Email

Your email is hardcoded to: **elenaeft07@gmail.com**

### Step 1: Load Environment Variables

```bash
source setup_pasqal.sh
```

This sets:

- `PASQAL_EMAIL=elenaeft07@gmail.com`
- `PASQAL_PROJECT_ID=591eb05c-88c2-4ca5-b228-5fd91e64855f`

### Step 2: Set Your Credentials

You have two options:

#### Option A: Use Your Actual Pasqal Credentials

If you have real Pasqal API credentials:

```bash
export PASQAL_CLIENT_ID="your-actual-client-id"
export PASQAL_CLIENT_SECRET="your-actual-client-secret"
```

#### Option B: Use Demo Mode

```bash
export PASQAL_CLIENT_ID="demo-client-id"
export PASQAL_CLIENT_SECRET="demo-client-secret"
```

### Step 3: Start the Web Server

```bash
cd site
python3 server.py
```

Visit http://localhost:8000 in your browser if you want to use the site backend. The studio opens with a Pasqal access panel, and you unlock it with the email and project ID configured in this repository.

---

## Usage in Java Code

Once environment variables are set:

```java
import com.gluonhq.strange.cloudlink.adapters.PasqualAdapter;
import org.redfx.strange.Program;

// The adapter automatically loads credentials from environment variables
PasqualAdapter adapter = new PasqualAdapter();

// Create your quantum program
Program program = /* ... */;

// Submit to Pasqual
Result result = adapter.submitProgram(program, 100); // 100 shots
```

---

## Alternative: Direct Instantiation

```java
// With explicit credentials
PasqualAdapter adapter = new PasqualAdapter(
    "your-client-id",
    "your-client-secret",
    "591eb05c-88c2-4ca5-b228-5fd91e64855f"
);
```

---

## Email-Based User Identification

To retrieve your Pasqal user ID using your email:

```java
import com.gluonhq.strange.cloudlink.adapters.PasqualAuthenticator;

// Create authenticator with your credentials
PasqualAuthenticator auth = new PasqualAuthenticator(
    "elenaeft07@gmail.com",
    "your-password"
);

// Get user ID
String userId = auth.getUserId();
System.out.println("Your Pasqal User ID: " + userId);

// Or get OAuth token
String token = auth.getOAuthToken(clientId, clientSecret);
```

---

## Troubleshooting

### "Pasqal credentials not configured"

Make sure you've run:

```bash
source setup_pasqal.sh
export PASQAL_CLIENT_ID="your-id"
export PASQAL_CLIENT_SECRET="your-secret"
```

### "Authentication failed"

- Double-check your Pasqal API credentials
- Verify your Project ID is correct
- Check that your email matches your Pasqal account

### Demo Mode Errors

If using demo credentials, the API calls will fail. Use real credentials from Pasqal Cloud:

1. Go to https://www.pasqal.io
2. Sign in with elenaeft07@gmail.com
3. Copy your Client ID and Secret
4. Set the environment variables

---

## Files

- `setup_pasqal.sh` - Quick environment setup script
- `src/main/java/com/gluonhq/strange/cloudlink/adapters/PasqualAdapter.java` - Main adapter
- `src/main/java/com/gluonhq/strange/cloudlink/adapters/PasqualAuthenticator.java` - Email auth utility
- `VAULT_CONFIGURATION.md` - Advanced vault setup
