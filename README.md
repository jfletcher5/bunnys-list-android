# Bunny's List Android

Native Android implementation of the Bunny's List grocery app (`com.bunnyslist.android`).

## Current scope

- Google sign-in with Firebase Auth and Credential Manager
- Sign-out, and an auth gate so the list is not shown signed out
- Real-time Firestore data under `users/{uid}/items` and `users/{uid}/categories`
- Search plus All / Needed filtering
- Add, edit, delete, and needed toggles
- Undo snackbar (about 5 seconds) on the Needed chip and on delete
- History screen that can reverse recent actions
- Changelog documents at `users/{uid}/actions`
- Offline banner
- Home-screen widget for needed items (requires a signed-in user)

## Stack

- Kotlin
- Jetpack Compose (Compose BOM 2026.06.01)
- Material 3
- Hilt, compiled with KSP
- Firebase Auth and Cloud Firestore (Firebase BOM 34.19.0; BOM 35 needs Kotlin 2.4, which has no KSP release yet)
- Credential Manager for Sign in with Google
- compileSdk / targetSdk 36

## Local setup

1. Install a JDK and Android SDK.
2. Copy `local.properties.example` to `local.properties`.
3. Set `sdk.dir` to your Android SDK path.
4. In the [Firebase console](https://console.firebase.google.com/) for project `bunny-listv2-d2rw2t`, register an Android app with package name `com.bunnyslist.android` if it is not there yet.
5. Download that app's `google-services.json` and save it as `app/google-services.json`. The file is gitignored. Do not commit it.
6. The file must include the Web client (`oauth_client` with `client_type` 3). The Google Services Gradle plugin turns that client into `R.string.default_web_client_id`. The app reads the web client id from that resource. It is not hardcoded.
7. `app/google-services.json.example` is only a structural placeholder so a debug build can compile. Replace it with the real download before trying to sign in.
8. Enable the Google provider under Authentication → Sign-in method.
9. Register the debug and release SHA-1 (and SHA-256) certificates under Project settings → Your apps. Debug SHA-1:

   ```bash
   ./gradlew :app:signingReport
   ```

   Add the SHA-1 shown for the `debug` variant, then re-download `google-services.json` if Firebase adds an Android OAuth client.

10. Build:

    ```bash
    ./gradlew :app:assembleDebug
    ```

## Where data lives

Signed-in users read and write only their own documents:

- `users/{uid}/items/{itemId}`
- `users/{uid}/categories/{categoryId}`
- `users/{uid}/actions/{actionId}`

New categories use a Firestore auto-id. Items store that document id in `itemCategory`. This replaces the old client-side `max(id) + 1`, which could assign the same numeric id from two devices.

Undo of a delete writes the item back with `set()` on the original document id. It does not call `add()`.

Each add, edit, delete, needed toggle, category create, and history reverse also writes an action document with:

- who (`actorUid`, `actorName`)
- what (`action`)
- item or category id (`targetType`, `targetId`)
- `before` and `after` snapshots when that side exists
- `createdAt` (server timestamp)

The History screen lists the latest 50 actions and can reverse one. Reverse writes `before` back onto the same id, or deletes the document when the original action created it.

## Migration and the household tradeoff

The previous Android and web clients used shared top-level collections: `items` and `categories`. Those paths are denied by `firestore.rules`.

**Do not deploy the rules until the data move below is done**, or the live web app and any already-installed build will lose access to the current list.

Suggested copy, once per account that should own the list:

1. Read each document in top-level `items` and `categories`.
2. Write it to `users/{uid}/items/{sameDocumentId}` and `users/{uid}/categories/{sameDocumentId}`. Keeping the document id lets item references and undo stay stable.
3. Keep a category's numeric `id` field if items still store that number in `itemCategory`. Android matches either the document id or that legacy number.
4. Copy only supported fields. Extra fields fail the rules on the next update.
   - Items: `name`, `description`, `itemCategory`, `isOwned`, `need`, `lastUpdated`
   - Categories: `itemCategory`, `lastUpdated`, and optional numeric `id`
5. Set `lastUpdated` with a server timestamp. The rules require it to equal `request.time`.
6. Update the web app to the same `users/{uid}/...` paths, or leave it offline until that ships.
7. Deploy rules only after that:

   ```bash
   npx -y firebase-tools@latest deploy --only firestore:rules --project bunny-listv2-d2rw2t
   ```

Per-user paths are the simple rule: the uid in the path is the owner, and one account cannot read another account. Bunny's List is a shared household list, so this is private rather than shared. A later `households/{householdId}` model with an explicit members map would let two people edit one list, but it needs invites and stricter rules. That is not in this change. If the household should keep a single shared list, migrate into the uid of the account everyone will sign in with, and plan the household model before a second account needs access.

`firestore.rules` are prototype rules. They check auth, ownership, field types, and size limits. Review them before a wider release. Syntax can be checked without deploying:

```bash
npx -y firebase-tools@latest emulators:exec --project demo-bunny-list --only firestore "echo rules-ok"
```

## Manual Firebase steps

Jon still needs to do these in the Firebase console for `bunny-listv2-d2rw2t`. They are not done by this repo:

1. Download a real `google-services.json` for package `com.bunnyslist.android` and put it at `app/google-services.json` (gitignored).
2. Confirm that file contains the Web OAuth client (`client_type` 3), so Sign in with Google can read `default_web_client_id`.
3. Add the app SHA-1 from `./gradlew :app:signingReport` to the Android app in Project settings.
4. Enable Google as an Authentication sign-in provider.
5. Copy existing `items` and `categories` under `users/{uid}/...` before deploying `firestore.rules`.
6. Review the prototype rules, then deploy them when the web client is ready for the new paths.
