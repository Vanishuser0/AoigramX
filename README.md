# Aoigram

An unofficial Telegram client for Android.

## Download

Requires Android 10 or later.

- [Telegram Channel](https://t.me/Aoigram)
- [GitHub Releases](https://github.com/risin42/Aoigram/releases)

## Compilation Guide

### Prerequisites

Android Studio (or the Android SDK with licenses accepted). Gradle provisions JDK 21, and the required SDK platform, NDK and CMake are installed automatically on first sync.

### Setup

1. Clone the repository along with its submodules:
   ```bash
   git clone --recursive --shallow-submodules https://github.com/risin42/Aoigram.git
   cd Aoigram
   ```

2. Go to the [Firebase Console](https://console.firebase.google.com/), create two Android apps with application IDs `nu.gpu.aoigram` and `nu.gpu.aoigram.debug`, enable Firebase Cloud Messaging, and download `google-services.json`.

3. Generate a signing keystore named `release.keystore` following the [Android App Signing guide](https://developer.android.com/studio/publish/app-signing).

4. Copy your `google-services.json` and `release.keystore` into `TMessagesProj_App/`.

5. Add your [Telegram API credentials](https://core.telegram.org/api/obtaining_api_id) and signing configuration to `local.properties` (or pass them as environment variables):
   ```properties
   APP_ID=
   APP_HASH=
   RELEASE_KEY_ALIAS=
   RELEASE_KEY_PASSWORD=
   RELEASE_STORE_PASSWORD=
   ```

6. *(Optional)* Replace the Google Maps API key in `TMessagesProj_App/src/main/AndroidManifest.xml`, and update the release channel constants (`CHANNEL_USERNAME`, `CHANNEL_ID`) in `TMessagesProj/src/main/java/nu/gpu/aoigram/remote/RemoteConfig.java`.

### Build

Run Gradle to assemble the release APK:

```bash
./gradlew :TMessagesProj_App:assembleRelease
```

By default, this builds for `arm64-v8a`. To target another architecture, set the `AOI_ABI` environment variable to `armeabi-v7a`, `x86`, `x86_64`, or `universal`.

## Localization

Translations are hosted on [Crowdin](https://crowdin.com/project/Aoigram). Contributions are welcome.
