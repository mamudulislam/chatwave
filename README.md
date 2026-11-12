# ChatWave

ChatWave is an Android messaging-app prototype built with Kotlin and Jetpack Compose. It includes screens for chats, contacts, groups, status stories, call history, profile, and privacy settings.

## Local data protection

The local repository writes its app state to a private encrypted snapshot. AES-256-GCM uses a fresh nonce for each write, and the key is generated and held by Android Keystore. Authentication failures and local persistence errors are not treated as successful reads or writes. Android backup is disabled because the device-bound key cannot decrypt a copied snapshot on another device.

This protects locally persisted data at rest. It is **not end-to-end encryption**: the current app has no authenticated peer-key exchange or encrypted network messaging. The sample contacts and conversations are local demo data.

## Backend and calling status

Supabase authentication, database/realtime messaging, encrypted media storage, and WebRTC signaling are not connected in this prototype. The call UI is a local preview; it does not establish a remote media connection. Do not use the current demo authentication, messages, or call screens as a production service.

To make the app production-ready, configure a Supabase project and deploy an appropriately restricted schema/policies, implement authenticated realtime synchronization and client-side message/media encryption with verified peer keys, and supply TURN credentials through a secure runtime configuration. Never put a Supabase service-role key or TURN secret in the Android app.

## Open and run

1. Open this directory in Android Studio and allow Gradle sync to complete.
2. Select an emulator or Android device and run the `app` configuration.
3. Run `gradle :app:assembleDebug` to build a debug APK.

## Build an APK with GitHub Actions

The `Build Android APK` workflow builds a debug APK on every branch push and pull request, and can also be started manually:

1. Push this repository to GitHub.
2. Open the repository's **Actions** tab and select **Build Android APK**. If prompted, enable Actions for the repository.
3. Wait for the workflow run to finish successfully.
4. Open that run and download the `ChatWave-debug-apk` artifact.
5. Extract the downloaded ZIP to get the APK.

The workflow installs Java, Gradle, and the Android SDK itself. It generates an ephemeral debug signing key for that CI run, so no signing secrets are needed to build or download the debug APK. This APK is for testing and is not a Play Store release. A production release requires a persistent, private upload key and protected GitHub Actions secrets.
