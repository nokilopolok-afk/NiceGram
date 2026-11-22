# NiceGram Next

This is an isolated Kotlin/Jetpack Compose app, so the existing NiceGram client remains untouched. The first UI preview now includes animated chat, archive, and settings screens, searchable demo conversations, a sample chat, and MTProto proxy-link parsing.

## Build

```powershell
.\gradlew.bat assembleDebug
```

GitHub Actions installs the Android 35 SDK components explicitly, runs the debug build, and uploads the APK when this directory changes on `main`, `master`, or `rewrite`.

The conversations are clearly marked as a preview: Telegram authorization, TDLib connectivity, persistent proxy storage, Room entities, and real chat data are not implemented yet. Proxy links are validated and kept in memory only; adding a proxy does not connect through it.

Message archival can only preserve content the app actually received before it disappears; it cannot recover messages deleted before installation or bypass Telegram's server-side behavior. Story publishing must follow Telegram's official eligibility checks; Premium or other server-side restrictions will not be bypassed.
