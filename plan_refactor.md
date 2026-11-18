# Refactoring plan: simpler, smaller, same app

Status: **draft for discussion**, nothing is built. Written 18 November 2026, after the owner asked to "simplify and
optimise the app once more". Each phase ships on its own with CI green; nothing here changes what families see unless
the phase says so.

## 1. Where the code is today

About 19,000 lines of Kotlin in 19 modules (plus `:app`). `:core:model` holds the rules and is well tested
(2,700 lines of code, 2,250 of tests). The weak spots:

| Finding | Where | Why it matters |
|---|---|---|
| Ten copies of "show a notification": permission check, channel, quiet hours, builder | `NewChoreAlerts`, `TimedReminders`, `WeeklySummaryWorker`, `ShoppingAlerts`, `MessageAlerts`, `JoinRequestAlerts`, `MorningSummaryWorker`, `PlaceAlertWorker`, `OnMyWayAlerts`, `LiveShareService` | About 200 lines repeated; a fix to one (say, quiet hours) has to be made ten times |
| Six copies of "a push ping starts a worker" | the `*Listener` classes in chores, groceries, map, family | The same 15 lines each |
| `filterIsInstance<SessionState.InFamily>()` + `flatMapLatest` at the top of 14 view models | every feature | Boilerplate, and each one decides on its own what happens on sign-out |
| Very long files | `TodayScreen.kt` 864 lines, `GroceriesScreen.kt` 636, `RoomsScreen.kt` 636, `ChoreEditorSheet.kt` 632, `LiveHouse.kt` 515, `MapScreen.kt` 501, `FamilyScreen.kt` 488 | Hard to read and review; every change touches a big file |
| Finishing a chore lives in `:feature:chores` (`ChoreChanger`) | chores | Casa and the widget can't offer "Done"; they send people to Corres |
| Old names | `TodayScreen`, `TODAY_ROUTE`, `todayScreen()` are the Chores tab | Confusing: "Today" is a sub-tab of Casa |
| Feature state classes are public | `ChoresUiState`, `MapUiState`, `GroceriesUiState`, ... | Wider API than the rule asks for |
| No tests at all | `:core:data` (Firestore mapping), `:core:settings`, `:core:push`, `:core:location`, `:core:update`, `:feature:onboarding` | Mapping mistakes only show on a real phone |
| Four plan files at the top of the repo | `PLAN.md`, `plan_home.md`, `plan_visual.md`, `plan_weather.md` | Three are finished; the decisions belong in `PLAN.md` |
| Six bottom tabs | Casa, Corres, Mercado, Mapa, Família, Ajustes | Crowded on small phones; labels already need ellipsis |
| Casa piles up cards | weather, urgent, getting started, the day, for you, worth a look, the day's list | Urgent chores show twice (urgent card and "For you") |

## 2. Phases

### R1. Clean-up with no visible change (low risk)

1. **One notifier.** `:core:settings` already owns which alerts a phone shows and the quiet hours, so it gets an
   `AlertNotifier.show(kind, channel, title, text, tapIntent)` that checks the permission, creates the channel, applies the
   alert switch and quiet hours, and posts. The ten callers shrink to one call each.
2. **One "ping starts work" helper** in `:core:push`: `PushWork.on(kind) { enqueue<Worker>(name) }`, so each feature's
   listener is two lines.
3. **`FamilyRepository.inFamily`**: one shared flow of the signed-in family, used by every view model.
4. **Names**: `TodayScreen` becomes `ChoresScreen`, `TODAY_ROUTE` becomes `CHORES_ROUTE`, and the like.
5. **State classes `internal`** where Hilt doesn't need them public (the view model exposes `internal val state`, as Casa
   already does).
6. **Docs**: fold the decisions of the three finished plans into `PLAN.md` and delete them; keep this file until R5 is done.

Expected: about 400 lines fewer, no screen changes. Screenshots must come out identical.

### R2. Split the long files (no visible change)

Move code only, no logic changes: `ChoresScreen` into the list, `ChoreCard` and the dialogs; `RoomsScreen` into list,
detail and dialogs; the chore editor into its sections; `GroceriesScreen`, `MapScreen` and `FamilyScreen` likewise.
Goal: no file over about 350 lines. Screenshots must come out identical.

### R3. Chore actions in one place (needs the owner's yes: a new core module)

Move `ChoreChanger` (apply a chore rule, save it, log a parent's finish, ping the family) into a new
**`:core:chores`** module that may depend on `:core:model`, `:core:data` and `:core:push`. It goes into
`ModuleRules.CORE_DEPENDENCIES` as a deliberate decision, as `CLAUDE.md` asks. With it:

- Casa's "For you today" gets the **Done** button from the mock-up;
- the widget can finish a chore without opening the app;
- the morning summary and the weekly summary read chores the same way the tabs do.

### R4. A simpler app for families (visible; each item needs the owner's yes)

1. **Five tabs instead of six.** Recommended: **Mapa** becomes a sub-tab of **Família** (people, where they are,
   notes). Casa, Corres, Mercado, Família, Ajustes. The two features stay separate modules; `:app` shows them under one
   tab with the shared `SubTabs` bar, so no feature depends on another. This adds one documented exception to the
   "one entry point" rule (each feature also exposes its screen for the shared tab), which is why it needs a yes.
2. **Fewer cards on Casa.** Urgent chores move into "For you today" (red rows at the top), and the separate urgent
   card only shows when the urgent ones are nobody's. The getting-started card folds into the day card as one line
   once half the steps are done.
3. **Merge `:feature:update` into `:feature:settings`.** It is 145 lines; the banner stays where it is.

### R5. Tests where there are none

Mapping tests for `:core:data` (Firestore fields to models and back, from plain maps, no emulator), and tests for
`AlertSettings`/quiet hours, `UnifiedPush` kinds, the update version check and the location sharing choice.

## 3. Not in this plan, on purpose

- **R8 shrinking**: worth doing only after testing on a real phone (it can break Hilt, Firebase and Firestore mapping).
- **Sharing Firestore listeners between screens**: the Firestore SDK already reuses one listener for identical
  queries, so reads (and the free quota) are not wasted today; sharing would only save a little mapping work.
- Rewriting the data layer or the navigation: they work, and the gain would not pay for the risk.

## 4. Order and size

| Phase | Visible change | Risk | Size |
|---|---|---|---|
| R1 | none | low | 1 to 2 sessions |
| R2 | none | low | 1 session |
| R3 | Done button on Casa (and widget, if wanted) | medium, new module | 1 session |
| R4 | five tabs, calmer Casa | medium, owner's choices | 1 to 2 sessions |
| R5 | none | low | 1 session |

Recommended order: R1, R2, R5 (so later phases are covered), then R3 and R4 after the owner decides.
