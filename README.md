# NiceGram for Android

NiceGram is an independent Telegram client based on the AyuGram4A source and Telegram TDLib sources.

- Repository: `nokilopolok-afk/NiceGram`
- Android package: `com.nicegram.app`
- Branding: NiceGram only
- Firebase Analytics and Crashlytics
- Telegram API credentials are never committed
- No unofficial Telegram verification badge is created
- Developer label: `NiceGram Developer`

## Requirements

- JDK 17
- Android SDK 33
- Android NDK 21.4.7075529
- A Telegram API ID and API hash obtained at https://my.telegram.org/apps
- A Firebase project for Analytics and Crashlytics
- An Android release keystore

## GitHub Secrets

Configure these repository secrets under **Settings > Secrets and variables > Actions**:

| Secret | Value |
|---|---|
| `NICEGRAM_API_ID` | Numeric Telegram API ID |
| `NICEGRAM_API_HASH` | Telegram API hash |
| `SIGNING_STORE_BASE64` | Base64-encoded release `.jks` |
| `SIGNING_STORE_PASSWORD` | Keystore password |
| `SIGNING_KEY_ALIAS` | Release key alias |
| `SIGNING_KEY_PASSWORD` | Release key password |
| `GOOGLE_SERVICES_JSON_BASE64` | Base64-encoded `google-services.json` |
| `MAPS_V2_API` | Optional Google Maps API key |

Create the keystore:

```powershell
keytool -genkeypair -v -keystore nicegram.jks -alias nicegram -keyalg RSA -keysize 2048 -validity 10000
```

Encode it locally:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("nicegram.jks"))
```

Firebase must use the package `com.nicegram.app`.

## Build

### GitHub Actions

Push to `main` or `master`, or run **Build NiceGram APK and AAB** manually from the Actions tab. Pushing to `rewrite` will not start the build automatically. Artifacts:

- `NiceGram-apk`: release universal APK
- `NiceGram-aab`: Android App Bundle

### Android Studio

1. Place `google-services.json` in `TMessagesProj/`.
2. Place `nicegram.jks` in `TMessagesProj/config/nicegram.jks`.
3. Create `API_KEYS` in the repository root:

```properties
APP_ID = YOUR_API_ID
APP_HASH = "YOUR_API_HASH"
MAPS_V2_API = YOUR_OPTIONAL_MAPS_KEY
SIGNING_KEY_STORE_PASSWORD = YOUR_STORE_PASSWORD
SIGNING_KEY_ALIAS = YOUR_KEY_ALIAS
SIGNING_KEY_PASSWORD = YOUR_KEY_PASSWORD
```

4. Open the repository in Android Studio and run `assembleAfatRelease`.

### Command line

```powershell
set NICEGRAM_API_ID=YOUR_API_ID
set NICEGRAM_API_HASH=YOUR_API_HASH
.\gradlew.bat assembleAfatRelease
.\gradlew.bat bundleAfatRelease
```

The generated files are in:

- `TMessagesProj/build/outputs/apk/afat/release/`
- `TMessagesProj/build/outputs/bundle/afatRelease/`

## NiceGram admin panel

Open the in-app admin deep link:

```text
nicegram://admin
```

It provides build/package information and indicates Firebase Analytics and Crashlytics integration. Detailed production statistics are available in the Firebase console.

## Profile additions

The profile design reserves a left-side status music slot and a right-side TON Connect slot. TON actions use the existing `ton:` intent support; wallet connections must be enabled by the account-specific TON service configuration.

## Credits

This project is a derivative build based on [AyuGram4A](https://github.com/AyuGram/AyuGram4A) and retains its applicable upstream licensing and attribution requirements.
