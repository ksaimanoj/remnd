#!/usr/bin/env python3
"""
build_and_upload.py
-------------------
Builds the remnd debug APK and uploads it to a Google Drive folder.

Requirements:
  pip install google-api-python-client google-auth-httplib2 google-auth-oauthlib

Google Drive setup (one-time):
  1. Go to https://console.cloud.google.com/
  2. Create a project → Enable "Google Drive API"
  3. Create OAuth 2.0 credentials (Desktop App)
  4. Download the credentials JSON → save as scripts/credentials.json
  5. Run this script once; it will open a browser to authorise and save token.json

Usage:
  python3 scripts/build_and_upload.py [--release] [--folder "remnd-builds"]
"""

import argparse
import os
import subprocess
import sys
from pathlib import Path

# ── Google Drive ──────────────────────────────────────────────────────────────
SCOPES = ["https://www.googleapis.com/auth/drive.file"]
CREDENTIALS_FILE = Path(__file__).parent / "credentials.json"
TOKEN_FILE = Path(__file__).parent / "token.json"


def get_drive_service():
    """Authenticate and return a Google Drive service object."""
    try:
        from google.oauth2.credentials import Credentials
        from google_auth_oauthlib.flow import InstalledAppFlow
        from google.auth.transport.requests import Request
        from googleapiclient.discovery import build
    except ImportError:
        print("❌  Missing dependencies. Run:")
        print("    pip install google-api-python-client google-auth-httplib2 google-auth-oauthlib")
        sys.exit(1)

    creds = None
    if TOKEN_FILE.exists():
        creds = Credentials.from_authorized_user_file(str(TOKEN_FILE), SCOPES)

    if not creds or not creds.valid:
        if creds and creds.expired and creds.refresh_token:
            creds.refresh(Request())
        else:
            if not CREDENTIALS_FILE.exists():
                print(f"❌  credentials.json not found at {CREDENTIALS_FILE}")
                print("    Download it from Google Cloud Console → APIs & Services → Credentials")
                sys.exit(1)
            flow = InstalledAppFlow.from_client_secrets_file(str(CREDENTIALS_FILE), SCOPES)
            creds = flow.run_local_server(port=0)
        with open(TOKEN_FILE, "w") as token:
            token.write(creds.to_json())

    return build("drive", "v3", credentials=creds)


def get_or_create_folder(service, folder_name: str) -> str:
    """Return the ID of a Drive folder, creating it if it doesn't exist."""
    query = (
        f"name='{folder_name}' and mimeType='application/vnd.google-apps.folder'"
        " and trashed=false"
    )
    results = service.files().list(q=query, fields="files(id, name)").execute()
    files = results.get("files", [])
    if files:
        folder_id = files[0]["id"]
        print(f"📁  Found existing folder '{folder_name}' (id: {folder_id})")
        return folder_id

    file_metadata = {
        "name": folder_name,
        "mimeType": "application/vnd.google-apps.folder",
    }
    folder = service.files().create(body=file_metadata, fields="id").execute()
    folder_id = folder["id"]
    print(f"📁  Created folder '{folder_name}' (id: {folder_id})")
    return folder_id


def upload_apk(service, apk_path: Path, folder_id: str) -> str:
    """Upload the APK to Drive and return its web link."""
    from googleapiclient.http import MediaFileUpload

    file_metadata = {"name": apk_path.name, "parents": [folder_id]}
    media = MediaFileUpload(str(apk_path), mimetype="application/vnd.android.package-archive")

    print(f"⬆️   Uploading {apk_path.name} ({apk_path.stat().st_size / 1_048_576:.1f} MB)…")
    uploaded = (
        service.files()
        .create(body=file_metadata, media_body=media, fields="id, webViewLink")
        .execute()
    )
    return uploaded.get("webViewLink", "")


# ── Build ─────────────────────────────────────────────────────────────────────

def build_apk(project_root: Path, release: bool) -> Path:
    """Run Gradle and return the path to the built APK."""
    gradlew = project_root / "gradlew"
    if not gradlew.exists():
        print(f"❌  gradlew not found at {gradlew}")
        sys.exit(1)

    task = "assembleRelease" if release else "assembleDebug"
    print(f"🔨  Running: ./gradlew {task}")

    result = subprocess.run(
        [str(gradlew), task, "--stacktrace"],
        cwd=str(project_root),
        capture_output=False,
    )
    if result.returncode != 0:
        print(f"❌  Gradle build failed (exit code {result.returncode})")
        sys.exit(result.returncode)

    variant = "release" if release else "debug"
    apk_dir = project_root / "app" / "build" / "outputs" / "apk" / variant
    apks = list(apk_dir.glob("*.apk"))
    if not apks:
        print(f"❌  No APK found in {apk_dir}")
        sys.exit(1)

    apk = max(apks, key=lambda p: p.stat().st_mtime)
    print(f"✅  Built: {apk}")
    return apk


# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description="Build remnd APK and upload to Google Drive")
    parser.add_argument("--release", action="store_true", help="Build release APK instead of debug")
    parser.add_argument("--folder", default="remnd-builds", help="Google Drive folder name (default: remnd-builds)")
    parser.add_argument("--skip-build", action="store_true", help="Skip the Gradle build (upload existing APK)")
    args = parser.parse_args()

    project_root = Path(__file__).parent.parent

    if args.skip_build:
        variant = "release" if args.release else "debug"
        apk_dir = project_root / "app" / "build" / "outputs" / "apk" / variant
        apks = list(apk_dir.glob("*.apk"))
        if not apks:
            print(f"❌  No pre-built APK found in {apk_dir}. Remove --skip-build to build first.")
            sys.exit(1)
        apk_path = max(apks, key=lambda p: p.stat().st_mtime)
        print(f"📦  Using existing APK: {apk_path}")
    else:
        apk_path = build_apk(project_root, args.release)

    print("\n🔑  Authenticating with Google Drive…")
    service = get_drive_service()

    folder_id = get_or_create_folder(service, args.folder)
    link = upload_apk(service, apk_path, folder_id)

    print(f"\n✅  Upload complete!")
    print(f"🔗  Google Drive link: {link}")


if __name__ == "__main__":
    main()
