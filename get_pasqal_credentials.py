import json
import requests
import sys
from getpass import getpass

def get_pasqal_credentials():
    """Retrieve user ID and credentials from Pasqal API"""
    
    print("=" * 60)
    print("Pasqal Credentials Retrieval")
    print("=" * 60)
    
    # getting email
    email = input("\nEnter your Pasqal Cloud email (elenaeft07@gmail.com): ").strip()
    if not email:
        email = "elenaeft07@gmail.com"
    
    # etting password securely
    password = getpass("Enter your Pasqal Cloud password: ")
    
    if not password:
        print("Error: Password is required")
        return
    
    try:
        # Authenticate and get user info
        auth_url = "https://apis.pasqal.cloud/api/v1/auth/info"
        
        response = requests.get(
            auth_url,
            auth=(email, password),
            timeout=10
        )
        
        if response.status_code == 401:
            print("\n❌ Authentication failed. Check your email/password.")
            return
        
        if response.status_code != 200:
            print(f"\n❌ API Error: {response.status_code}")
            print(f"Response: {response.text}")
            return
        
        data = response.json()
        
        print("\n" + "=" * 60)
        print("✅ Successfully retrieved your Pasqal credentials:")
        print("=" * 60)
        
        # Extract and display credentials
        user_id = data.get("user_id", data.get("id", "N/A"))
        print(f"\n👤 User ID: {user_id}")
        print(f"📧 Email: {email}")
        
        # Display projects if available
        if "projects" in data:
            print(f"\n📋 Projects ({len(data['projects'])}):")
            for project in data["projects"]:
                project_id = project.get("id", "N/A")
                project_name = project.get("name", "Unnamed")
                print(f"   - {project_name}: {project_id}")
        
        # Generate environment variable export command
        print("\n" + "=" * 60)
        print("Set these environment variables:")
        print("=" * 60)
        
        print(f'\nexport PASQAL_EMAIL="{email}"')
        print(f'export PASQAL_USER_ID="{user_id}"')
        print(f'export PASQAL_PROJECT_ID="591eb05c-88c2-4ca5-b228-5fd91e64855f"')
        
        # For security, ask about storing credentials in vault
        print("\n" + "=" * 60)
        print("Security Recommendation:")
        print("=" * 60)
        print("""
For production use, store these securely in:
1. Azure Key Vault
2. HashiCorp Vault
3. AWS Secrets Manager

Do NOT commit credentials to version control!
        """)
        
    except requests.exceptions.ConnectionError:
        print("\n❌ Connection error: Could not reach Pasqal API")
        print("Make sure you have internet connection and Pasqal is accessible")
    except requests.exceptions.Timeout:
        print("\n❌ Request timeout: Pasqal API did not respond in time")
    except json.JSONDecodeError:
        print("\n❌ Invalid response format from Pasqal API")
    except Exception as e:
        print(f"\n❌ Error: {str(e)}")

if __name__ == "__main__":
    get_pasqal_credentials()
