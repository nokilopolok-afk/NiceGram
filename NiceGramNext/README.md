# NiceGram Next

This is an isolated Kotlin/Jetpack Compose app, so the existing NiceGram client remains untouched. The animated violet-and-red UI preview includes chat, archive, and settings screens, searchable demo conversations, a sample chat, and MTProto proxy-link parsing.

## Build

```powershell
.\gradlew.bat assembleDebug
```

GitHub Actions installs the Android 35 SDK components explicitly, runs the debug build, and uploads the APK when this directory changes on `main`, `master`, or `rewrite`.

The app now uses TDLib for phone/code/two-step-password authorization, live chat history and sending messages. MTProto proxy links are added to the active TDLib session. The native TDLib AAR is pinned to `io.github.tdlib-android:core:0.1.1`, a community-built Android package of the official TDLib; review its provenance before production distribution. It requires Android 8.0 (API 26) or later.

Local message archiving is opt-in and disabled by default. It records only incoming messages received after enabling it and observed by the app before deletion; it cannot recover older messages or bypass Telegram's server-side behavior. Story publishing must follow Telegram's official eligibility checks; Premium or other server-side restrictions will not be bypassed.

Set `NICEGRAM_API_ID` and `NICEGRAM_API_HASH` as local Gradle environment variables or GitHub Actions secrets. Obtain credentials from [my.telegram.org](https://my.telegram.org); do not commit them or post them in chat.
