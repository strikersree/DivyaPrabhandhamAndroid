# Making Google sync (and Ask history) actually work: what to configure

The app's sync uses Google's Identity **AuthorizationClient** to request the
Drive **appdata** scope. Two OAuth clients are needed together: an **Android**
client, matched by the app's package name + signing SHA-1 (no ID in source for
this one), and a **Web application** client, whose ID *does* go in source
(`GoogleSyncManager.WEB_CLIENT_ID`) via `requestOfflineAccess()` — this is
what makes the authorization survive app restarts, rather than needing
re-consent every cold start. Without the Web client, sign-in can still
complete once but "signs in but never syncs" again after the app is killed and
reopened (now shown as "Sync failed — tap to retry").

The Ask history feature uses the **same** Drive appdata scope, so configuring
this once fixes both.

## What to set up (one-time, in Google Cloud Console)

1. **A Google Cloud project** (or reuse the one the Gemini proxy lives in:
   `spry-guru-504101-r5`).

2. **Enable the Google Drive API** for that project.
   APIs & Services -> Library -> "Google Drive API" -> Enable.

3. **Configure the OAuth consent screen** (External), add the scope
   `.../auth/drive.appdata`, and add yourself as a test user while it is in
   testing. (Publishing is only needed for other users; for your own testing,
   test-user is enough.)

4. **Create an Android OAuth client ID.**
   APIs & Services -> Credentials -> Create credentials -> OAuth client ID ->
   Android. It needs:
   - **Package name:** `com.srinivaskannan.divyaprabhandham`
   - **SHA-1 certificate fingerprint:** the signing cert of the build on the
     device. For a debug build:
     `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android`
     and copy the SHA1 line. For a Play/release build, use the SHA-1 of the
     release keystore **and** the Play App Signing SHA-1 (Play Console -> your
     app -> Setup -> App signing).

5. **Create a Web application OAuth client ID**, same Credentials page ->
   Create credentials -> OAuth client ID -> Web application. No redirect URI
   or origin needed for this use — just create it and copy the client ID
   (`<numbers>-<random>.apps.googleusercontent.com`) into
   `GoogleSyncManager.WEB_CLIENT_ID`. This ID is public by design (Google
   distributes it embedded in client code); its *secret* counterpart must
   never go in the app, and this setup never needs that secret.

The Android client matches package + SHA-1 at runtime, and the Web client ID
in source is what `requestOfflineAccess()` uses to keep the grant usable
across app restarts. With both in place, sign-in yields a usable Drive token
and both sync and Ask history start working.

## Do I need Firebase?

**Not for sync or Ask history.** Those only need the two Cloud OAuth clients
above.

**Firebase is only for App Check** (Phase 3 of the Ask proxy) — the piece that
stops someone calling your Gemini endpoint from outside the app. That is
independent of sync. When you get to it:
- Add the app to a Firebase project (same Cloud project is fine).
- Enable **App Check** with the **Play Integrity** provider.
- Redeploy the proxy with `ENFORCE_APP_CHECK=true` and have the app attach the
  App Check token (a small client addition; the proxy already reads the header).

Until you enable App Check, the proxy works with `ENFORCE_APP_CHECK` unset — the
Ask feature you tested does not depend on Firebase.

## Common gotchas (the reasons sync silently fails)

- **SHA-1 mismatch:** the OAuth client's SHA-1 must match the exact build on the
  device. Debug build on your Pixel -> debug SHA-1. Installing a Play build
  later -> add the Play App Signing SHA-1 too. A mismatch = unusable grant.
- **Wrong package name:** must be exactly `com.srinivaskannan.divyaprabhandham`.
- **Drive API not enabled:** the token is issued but Drive calls 403.
- **Not added as a test user** while the consent screen is in testing.
- Changes can take a few minutes to propagate.

## How to confirm it worked

On the device, Settings -> sync -> tap Sync. The row should move from
"Sync failed" to a timestamp ("synced just now"). Then open Ask, ask a question,
and the History button (top-right, visible when signed in) should show it.
