# NiceGram Next

This is an isolated Kotlin/Jetpack Compose application scaffold. It keeps the existing NiceGram client intact while the new client is developed in phases.

## Build

```powershell
.\gradlew.bat assembleDebug
```

GitHub Actions also builds and uploads a debug APK when this directory changes on `main`, `master`, or `rewrite`.

The current phase provides the Android app foundation, a dark Compose shell, Room dependencies, and a standalone Gradle wrapper. Telegram authorization, TDLib integration, database entities, and chat features are subsequent phases.

Story publishing must follow Telegram's official eligibility checks. The client will not bypass Premium or other server-side restrictions.
