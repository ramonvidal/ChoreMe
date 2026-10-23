# ChoreMe

An Android app for organizing a family: shared chores, a grocery list built around
the things you buy every week, and your family Google Calendar, all synced across
everyone's phones.

See [PLAN.md](PLAN.md) for the roadmap and design decisions.

## Tech stack

Kotlin, Jetpack Compose (Material 3), Hilt, Room, and Firebase (Auth, Firestore,
Cloud Messaging). Minimum Android 8.0 (API 26).

## Getting started

1. Install [Android Studio](https://developer.android.com/studio) and open this folder.
2. Set up Firebase (needed for sync; the app builds and runs without it):
   1. Create a project at <https://console.firebase.google.com> (the free Spark plan is enough).
   2. Add an **Android app** with package name `com.choreme`.
   3. Download `google-services.json` and put it in `app/`. It is git-ignored on purpose.
   4. Enable **Authentication → Google** and create a **Firestore** database.
3. Run the `app` configuration on a device or emulator.

## Downloading a build without Android Studio

Every push runs the **Android build** GitHub Action. Open the run under the
repository's *Actions* tab and download the `choreme-debug-apk` artifact, then
install it on your phone (allow installing from unknown sources).

CI builds don't include `google-services.json`, so sync is off in those APKs for now.

## Project layout

```
app/src/main/java/com/choreme/
  domain/model/   Plain Kotlin models and rules (e.g. kid chores need a parent's approval)
  data/local/     Room database (offline storage)
  di/             Hilt modules (database, Firebase)
  feature/        One package per screen: today, groceries, agenda, family
  ui/             Theme, navigation, shared composables
```
