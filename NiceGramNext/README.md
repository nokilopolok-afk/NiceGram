# NiceGram Next

This is an isolated Kotlin/Jetpack Compose app, so the existing NiceGram client remains untouched. It provides an animated violet-and-red interface for TDLib authorization, live chats, Telegram chat folders, account profile, accessible active stories, stickers, a local deleted-message archive, and settings.

## Build

```powershell
.\gradlew.bat assembleDebug
```

GitHub Actions installs the Android 35 SDK components explicitly, runs the debug build, and uploads the APK when this directory changes on `main`, `master`, or `rewrite`.

The app uses TDLib for phone/code/two-step-password authorization, live chat history and sending messages. It reads chat folders and account details from the signed-in account, lists accessible active stories, and displays story photos/videos and sticker media supported by Android; unsupported animated sticker formats fall back to their emoji. MTProto proxy links are added to the active TDLib session; managing multiple proxies and automatic failover are not implemented yet. The native TDLib AAR is pinned to `io.github.tdlib-android:core:0.1.1`, a community-built Android package of the official TDLib; review its provenance before production distribution. It requires Android 8.0 (API 26) or later.

Local message archiving is opt-in and disabled by default. It records only incoming messages received after enabling it and observed by the app before deletion; archived message text and the TDLib database key are protected with an Android Keystore AES-GCM key. When TDLib reports a deletion, an already-loaded message is marked in the open chat and its locally archived record can be opened from the long-press history sheet. The displayed deletion time is when this client observed the update, not a server-provided timestamp. This cannot recover older messages or bypass Telegram's server-side behavior. Story publishing must follow Telegram's official eligibility checks; Premium or other server-side restrictions will not be bypassed.

Set `NICEGRAM_API_ID` and `NICEGRAM_API_HASH` as local Gradle environment variables or GitHub Actions secrets. Obtain credentials from [my.telegram.org](https://my.telegram.org); do not commit them or post them in chat.
