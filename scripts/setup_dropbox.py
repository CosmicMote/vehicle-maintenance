#!/usr/bin/env python3
"""
One-time script to obtain Dropbox OAuth2 credentials for the backup feature.

Steps:
  1. Go to https://www.dropbox.com/developers/apps and create a new app.
     - Choose "Scoped access" → "Full Dropbox" (or "App folder" for isolation).
     - Under Permissions, enable: files.content.write, files.content.read.
  2. Note your App key and App secret from the Settings tab.
  3. Run this script:
       python scripts/setup_dropbox.py
  4. Copy the printed env vars into your .env file or Docker environment.
"""

import sys

try:
    from dropbox import DropboxOAuth2FlowNoRedirect
except ImportError:
    print("ERROR: 'dropbox' package not found. Install it with:")
    print("  pip install dropbox")
    sys.exit(1)

print("=== Dropbox OAuth2 Setup ===\n")
app_key = input("Dropbox App Key:    ").strip()
app_secret = input("Dropbox App Secret: ").strip()

auth_flow = DropboxOAuth2FlowNoRedirect(
    app_key,
    app_secret,
    token_access_type="offline",   # offline = gets a long-lived refresh token
)

authorize_url = auth_flow.start()
print(f"\n1. Open this URL in your browser:\n\n   {authorize_url}\n")
print("2. Click 'Allow', then copy the authorization code shown.\n")
auth_code = input("Authorization code: ").strip()

try:
    result = auth_flow.finish(auth_code)
except Exception as exc:
    print(f"\nERROR: {exc}")
    sys.exit(1)

print("\n=== Success! Add these to your .env file or Docker environment: ===\n")
print(f"DROPBOX_APP_KEY={app_key}")
print(f"DROPBOX_APP_SECRET={app_secret}")
print(f"DROPBOX_REFRESH_TOKEN={result.refresh_token}")
print()
print("Optional (defaults shown):")
print("DROPBOX_BACKUP_PATH=/vehicle-maintenance/vehicle_maintenance.db")
print("BACKUP_INTERVAL_HOURS=24")
