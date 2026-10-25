# ChoreMe

An Android app for organizing a family: shared chores, a grocery list built around
the things you buy every week, and your family Google Calendar, all synced across
everyone's phones.

See [PLAN.md](PLAN.md) for the roadmap and design decisions.

## Installing on a phone

Download **[ChoreMe.apk](https://github.com/ramonvidal/ChoreMe/releases/latest/download/ChoreMe.apk)**
on the phone and open it (allow installing from unknown sources). That link always
points at the newest build and needs no GitHub account. Each build installs over the
previous one. After that, ChoreMe checks for new builds itself (every few hours, or right away from **Family → This phone → Check for updates**) and offers
to install them: tap **Install** on the banner and confirm.

## Tech stack

Kotlin, Jetpack Compose (Material 3), Hilt, and Firebase (Auth, Firestore,
Cloud Messaging). Minimum Android 8.0 (API 26).

## Setting up Firebase (one time)

Sync and sign-in need a Firebase project. The free Spark plan is enough.

1. Create a project at <https://console.firebase.google.com>.
2. **Authentication → Sign-in method**: enable **Google** and **Anonymous**
   (anonymous is how kids without a Google account sign in).
3. **Firestore Database**: create a database (production mode, any nearby region).
4. **Firestore → Rules**: replace the rules with the contents of
   [`firebase/firestore.rules`](firebase/firestore.rules) and publish. Or, with the
   Firebase CLI: `cd firebase && npx firebase deploy --only firestore:rules --project <your-project-id>`.
5. **Project settings → Your apps → Add app → Android**, package name `com.choreme`.
6. Google sign-in only works for apps signed with a key whose **SHA-1** is
   registered there, so everyone uses one shared debug key:
   ```sh
   keytool -genkeypair -v -keystore app/debug.keystore -storepass android -keypass android \
     -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
   keytool -list -v -keystore app/debug.keystore -storepass android | grep SHA1
   ```
   Add that SHA-1 to the Android app in Firebase. `app/debug.keystore` is git-ignored;
   keep a copy somewhere safe.
7. Download `google-services.json` (after adding the SHA-1) into `app/`. It is git-ignored too.

### Building in GitHub Actions

Every push runs the **Android build** workflow, which uploads a `choreme-debug-apk`
artifact you can install on a phone (allow installing from unknown sources). For
that APK to sync and sign in, add two repository secrets under *Settings → Secrets
and variables → Actions*:

| Secret | Value |
|---|---|
| `GOOGLE_SERVICES_JSON` | The full contents of `google-services.json` |
| `DEBUG_KEYSTORE_BASE64` | The output of `base64 -w0 app/debug.keystore` |

Without them the APK still builds, but shows a "Sync isn't set up" screen.

### Building locally

Open the folder in [Android Studio](https://developer.android.com/studio), with
`google-services.json` and `debug.keystore` in `app/`, and run the `app` configuration.

## Instant alerts (optional, free)

Install the free **ntfy** app from Google Play on each phone that should get instant
arrival alerts. No account or settings needed: ChoreMe finds it and connects by itself.
Phones without it still get alerts, within about 15 minutes.

## Tests

- App logic: `./gradlew :core:model:test testDebugUnitTest`
- Module structure rules: `./gradlew -p build-logic :rules:test`
- Firestore security rules (needs Node.js and Java): `cd firebase && npm ci && npm test`

The UI tests also save screenshots of the screens; every CI run uploads them as the
`screenshots` artifact.

## Icons

Chore and grocery pictures come from [Tabler Icons](https://tabler.io/icons) (MIT) and
app controls from [Material Symbols](https://fonts.google.com/icons) (Apache 2.0). Their
licenses ship with the app in `core/ui/src/main/assets/licenses`.

## Project layout

The app is split into small Gradle modules; the rules for what may depend on what are
in [CLAUDE.md](CLAUDE.md) and are checked on every build.

```
app/              Activity, root screen, bottom navigation: wires the features together
core/model/       Plain Kotlin models and rules (chores, sessions, groceries), no Android
core/data/        Firebase repositories: auth, families, groceries
core/ui/          Theme, shared composables, shared strings and error messages
core/calendar/    The phone's calendars: read events, add events with alerts
core/location/    The phone's position, location permissions, the "share my location" switch
core/push/        Free instant pings between family phones (UnifiedPush, e.g. the ntfy app)
feature/
  onboarding/     Sign in, create or join a family, wait for approval
  chores/         Today tab
  groceries/      Grocery list
  agenda/         Family calendar and chores, day by day
  map/            Where everyone who shares is, on OpenStreetMap
  family/         Members, invite code, join approvals
build-logic/
  convention/     Shared Gradle setup for each kind of module
  rules/          The module structure check
firebase/
  firestore.rules Who can read and write what; tested in firebase/test
```
