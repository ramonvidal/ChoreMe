# ChoreMe: project contract

Rules every change must follow. Product decisions and the roadmap live in `PLAN.md`;
setup and build instructions in `README.md`.

## 1. Modular structure (the owner's standing requirement)

Keep the code as modular as possible: small Gradle modules with one job each and
narrow public APIs. **Breaking the structure is never a silent side effect of
another change.** If a task seems to need it, stop and tell the user what rule is
in the way and what you propose, before writing the code.

### Module structure

| Module | Job | May depend on |
|---|---|---|
| `:core:model` | Models and rules in **plain Kotlin** (no Android): chores, members, sessions, groceries | nothing |
| `:core:data` | Firebase: auth and Firestore repositories | `:core:model` |
| `:core:ui` | Theme, shared composables, strings used by more than one feature, error messages | `:core:model` |
| `:core:calendar` | The phone's calendars (Android `CalendarContract`): read events, add events with alerts | `:core:model` |
| `:core:location` | The phone's own position (fused location provider), location permissions, the "share my location" switch | `:core:model` |
| `:core:push` | Free instant wake-up pings between family phones (UnifiedPush, e.g. the ntfy app). Pings carry no family data; features react through `PushListener` | `:core:model` |
| `:feature:<name>` | One tab or flow: `onboarding`, `chores`, `groceries`, `agenda`, `map`, `family` | `:core:*` only |
| `:app` | Wires features together: activity, root screen, navigation | anything; nothing depends on it |

- **Features never depend on each other.** What two features share moves down into
  a `:core` module.
- **Each feature exposes one entry point**, its `XxxNavigation.kt` (a route constant
  and a `NavGraphBuilder` extension). Screens and helpers are `internal`; view models
  stay public because Hilt needs them to be. Known exceptions, keep the list short:
  - `:feature:onboarding`: `:app` shows its screens directly, outside navigation.
  - `:feature:chores`: `ChoreReminders.schedule()`, which `:app` calls at startup.
  - `:feature:map`: `PlaceAlerts.schedule()`, which `:app` calls at startup, so every
    phone gets arrival alerts, even one that never opened the map.
- **Features talk to each other only through `:app`.** If one needs to send people to
  another (Today linking to the grocery list), it takes a callback like
  `onOpenGroceries` and `:app` does the navigation.
- **Logic goes in `:core:model`** whenever it doesn't need Android, so it stays testable
  on the plain JVM. View models orchestrate; they don't hold business rules.
- **Each module owns its strings.** Strings used by one feature live in that feature;
  shared ones (Cancel, Save, error messages) live in `:core:ui` and are imported as
  `com.choreme.core.ui.R as CoreUiR`.
- **New modules use the convention plugins** in `build-logic/convention`
  (`choreme.android.feature`, `choreme.android.library`, `choreme.jvm.library`, ...),
  so their build files stay a few lines long.
- **New core modules** must be added to `ModuleRules.CORE_DEPENDENCIES` with what they
  may depend on. That's a deliberate decision, not a workaround.

### How breaking it is caught

`build-logic/rules` (`choreme.module-rules`, applied in the root `build.gradle.kts`)
checks these rules every time Gradle configures the project, so **any build, locally
or in CI, fails** with a message naming the broken rule. Its own tests:
`./gradlew -p build-logic :rules:test`.

Never weaken or bypass the check to get a build green. When reviewing or changing
code, also watch for what the check can't see, and point it out:
- feature code that's really shared (copy-pasted between features),
- business rules sitting in view models or composables instead of `:core:model`,
- public declarations in a feature beyond its navigation entry point,
- Firebase types leaking out of `:core:data`.

## 2. Security and data

- Every Firestore collection or field change comes with a rule in
  `firebase/firestore.rules` **and** emulator tests in `firebase/test/`. Access to a
  family is decided only by `families/{f}/memberUids/{uid}`.
- Collection and field names live in `FirestoreSchema` and must match the rules.
- Kids can't approve, manage members or change roles; keep it that way in both the
  rules and the UI.

## 3. Checks before pushing

- `./gradlew :core:model:test testDebugUnitTest` (`:core:model` is plain Kotlin, so its
  tests run under `test`, not `testDebugUnitTest`)
- `./gradlew assembleDebug`
- `cd firebase && npm test` when rules change
- `./gradlew -p build-logic :rules:test` when the module rules change

CI runs all of these. The Android SDK can't be downloaded in Claude's cloud sessions,
so there CI is the only full Android build: don't call an Android change done until
CI is green.
