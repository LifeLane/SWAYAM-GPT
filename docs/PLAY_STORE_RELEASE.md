# Google Play Store Release & Permanent Upload Key Guide

This guide explains how to manage, configure, and secure the permanent Google Play upload key for automated GitHub Actions release builds in **SWAYAM-GPT**.

---

## 1. What is the Google Play Store Upload Key?

When publishing an Android application to the Google Play Store, Google uses **Google Play App Signing**:
- Developers sign release Android App Bundles (`.aab`) or APKs with a private **Upload Key**.
- When Google Play receives the bundle, it verifies the signature with your registered upload key, removes the upload signature, and signs the app with Google's master app-signing key for distribution to users.

### Why Must the Upload Key Be Kept Permanently?
Once an app is registered with an upload key on Google Play Console:
- **Google Play will strictly reject any future app updates that are signed with a different key.**
- **Never generate a new keystore on each CI run.**
- The upload keystore file (`.jks`) must be generated once and permanently preserved.

---

## 2. Required GitHub Secrets

For release builds to succeed in GitHub Actions (`.github/workflows/build.yml`), configure the following three repository secrets under **Settings > Secrets and variables > Actions**:

| Secret Name | Description |
|---|---|
| `KEYSTORE_BASE64` | The complete permanent `.jks` file converted into a single-line Base64 string. |
| `STORE_PASSWORD` | The password protecting the `.jks` keystore file. |
| `KEY_PASSWORD` | The password protecting the specific private key alias (`upload`). |

> **Security Mandate**: Never print, echo, commit, or share these secrets or passwords in Git repositories, pull requests, issue trackers, or build logs.

---

## 3. How to Generate and Base64-Encode the Permanent Upload Keystore

### Step A: Generate the Keystore (One-Time Creation)
If you do not already have an upload keystore for this application, generate it once locally:

```bash
keytool -genkeypair \
  -v \
  -keystore my-upload-key.jks \
  -alias upload \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -dname "CN=SWAYAM-GPT Upload Key,O=LifeLane,C=IN"
```
*(Choose secure passwords for the keystore and alias key, and record them in your password manager.)*

### Step B: Convert the Keystore to a Base64 String

- **Linux**:
  ```bash
  base64 -w 0 my-upload-key.jks > keystore_base64.txt
  ```

- **macOS**:
  ```bash
  base64 -i my-upload-key.jks | tr -d '\n' > keystore_base64.txt
  ```

- **Windows (PowerShell)**:
  ```powershell
  [Convert]::ToBase64String([IO.File]::ReadAllBytes("my-upload-key.jks")) | Out-File -Encoding ascii keystore_base64.txt
  ```

---

## 4. How to Add Secrets to GitHub

1. Open your repository on GitHub.
2. Navigate to **Settings** > **Secrets and variables** > **Actions**.
3. Click **New repository secret**.
4. Add each secret:
   - **`KEYSTORE_BASE64`**: Paste the entire string from `keystore_base64.txt` (ensure no leading/trailing whitespace).
   - **`STORE_PASSWORD`**: Enter your keystore password.
   - **`KEY_PASSWORD`**: Enter your key alias password.
5. After saving the secrets, permanently and securely delete `keystore_base64.txt` from your local disk.

---

## 5. Where the Permanent Keystore Should Be Backed Up

- **Encrypted Password Manager / Vault**: Store `my-upload-key.jks` along with its alias and passwords in a secure company vault (e.g. 1Password, Bitwarden, HashiCorp Vault, or Google Cloud Secret Manager).
- **Cold Storage Backup**: Retain an encrypted offline copy.
- **Never commit `.jks` files, `.keystore` files, or passwords to Git.**

---

## 6. What NOT to Commit to GitHub

The `.gitignore` file enforces that the following sensitive files are never tracked:
- `*.jks`, `*.keystore`, `*.p12`, `*.pepk`
- `my-upload-key.jks`
- `local.properties`
- `.env` (contains API keys and runtime secrets)
- Hardcoded passwords in Gradle build files or source code

---

## 7. How the CI/CD Release Workflow Operates

When a tag (`v*`), push to `main`/`master`, or manual `workflow_dispatch` triggers `.github/workflows/build.yml`:
1. **Validation & Fail Fast**: The workflow checks if `KEYSTORE_BASE64`, `STORE_PASSWORD`, and `KEY_PASSWORD` are present. If any are missing, the build terminates immediately with an error message without generating a fake key.
2. **Reconstruction**: The runner decodes `KEYSTORE_BASE64` into a temporary `my-upload-key.jks` file.
3. **Compilation & Signing**:
   - `assembleRelease` compiles and signs the release APK.
   - `bundleRelease` compiles and signs the release Android App Bundle (`.aab`).
4. **Artifact Upload**:
   - `swayam-gpt-apk`: Release APK for internal distribution and testing.
   - `swayam-gpt-aab`: Release Android App Bundle ready for Google Play Console upload.
5. **Secure Cleanup**: An `if: always()` step guarantees that `my-upload-key.jks` is immediately deleted from the CI runner.

---

## 8. Uploading to Google Play Console

1. Download the `swayam-gpt-aab` artifact from the GitHub Actions run.
2. Log in to [Google Play Console](https://play.google.com/console).
3. Select your application and navigate to **Release > Production** (or **Testing > Closed testing**).
4. Create a new release and upload the `.aab` file from the downloaded artifact.
5. If releasing for the first time, enroll in **Google Play App Signing** using this upload key.
