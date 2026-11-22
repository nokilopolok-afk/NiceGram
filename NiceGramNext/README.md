# NiceGram Next

This is an isolated Kotlin/Jetpack Compose app, so the existing NiceGram client remains untouched. It opens with the uploaded NiceGram logo and a Start button, then runs TDLib phone/code/two-step-password authorization. After login it uses a light, Telegram-inspired chat list, profile, and settings layout with the NiceGram blue accent; custom features remain grouped separately from the standard account controls.

The developer panel is shown only when the signed-in Telegram user ID is `6226314827`; a username is not used as an identity check. It displays local client statistics and non-sensitive in-memory diagnostic events, can refresh chats/stories, and can clear only the app's temporary cache after confirmation. It does not grant server-side privileges or alter Telegram account data.

## Build

```powershell
.\gradlew.bat assembleDebug
```

GitHub Actions installs the Android 35 SDK components explicitly, runs the debug build, and uploads the APK when this directory changes on `main`, `master`, or `rewrite`.

The app uses TDLib for phone/code/two-step-password authorization, live chat history and sending messages. It reads chat folders and account details from the signed-in account, lists accessible active stories, and displays story photos/videos and sticker media supported by Android; unsupported animated sticker formats fall back to their emoji. MTProto proxy links are added to the active TDLib session; managing multiple proxies and automatic failover are not implemented yet. The native TDLib AAR is pinned to `io.github.tdlib-android:core:0.1.1`, a community-built Android package of the official TDLib; review its provenance before production distribution. It requires Android 8.0 (API 26) or later.

Local message archiving is opt-in and disabled by default. It records only incoming messages received after enabling it and observed by the app before deletion; archived message text and the TDLib database key are protected with an Android Keystore AES-GCM key. When TDLib reports a deletion, an already-loaded message is marked in the open chat and its locally archived record can be opened from the long-press history sheet. The displayed deletion time is when this client observed the update, not a server-provided timestamp. This cannot recover older messages or bypass Telegram's server-side behavior. Story publishing must follow Telegram's official eligibility checks; Premium or other server-side restrictions will not be bypassed.

Ghost mode sets TDLib's `online` option to false while enabled and this client does not call TDLib's `ViewMessages` method when opening chat history. It is intended to keep the account offline and avoid this client explicitly marking fetched messages as read; Telegram may still apply server-side presence/read behavior, and the feature cannot hide activity performed in other clients or override account privacy rules.

Set `NICEGRAM_API_ID` and `NICEGRAM_API_HASH` as local Gradle environment variables or GitHub Actions secrets. Obtain credentials from [my.telegram.org](https://my.telegram.org); do not commit them or post them in chat.

The welcome and sign-in screens load the NiceGram logo from `app/src/main/assets/NiceGram_icon_512_transparent.png`. Replace that file to change the logo; a square transparent PNG is recommended.
