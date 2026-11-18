# Refactoring plan: simpler, smaller, same app

Status: **agreed; R1, R2 and U1 done 18 November 2026** (CI green; Undo also on Rooms). R3 built (waiting for CI); then U2 and R5. Interface items (U1, U2) added the same day; the owner approved R3 and turned down R4. Written 18 November 2026, after the owner asked to "simplify and
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
2. ~~One "ping starts work" helper~~: **dropped**. Measured again, only three listeners repeat the enqueue (about 15
   lines), and a helper would put WorkManager into `:core:push`. Not worth it.
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

### R3. Chore actions in one place (approved by the owner, 18 November 2026: a new core module)

Move `ChoreChanger` (apply a chore rule, save it, log a parent's finish, ping the family) into a new
**`:core:chores`** module that may depend on `:core:model`, `:core:data` and `:core:push`. It goes into
`ModuleRules.CORE_DEPENDENCIES` as a deliberate decision, as `CLAUDE.md` asks. With it:

- Casa's "For you today" gets the **Done** button from the mock-up;
- the widget can finish a chore without opening the app;
- the morning summary and the weekly summary read chores the same way the tabs do.

### R4. A simpler app for families: **not doing** (the owner said no, 18 November 2026)

Kept here as a record. Six tabs stay, Casa keeps its cards, `:feature:update` stays its own module.

1. **Five tabs instead of six.** Recommended: **Mapa** becomes a sub-tab of **Família** (people, where they are,
   notes). Casa, Corres, Mercado, Família, Ajustes. The two features stay separate modules; `:app` shows them under one
   tab with the shared `SubTabs` bar, so no feature depends on another. This adds one documented exception to the
   "one entry point" rule (each feature also exposes its screen for the shared tab), which is why it needs a yes.
2. **Fewer cards on Casa.** Urgent chores move into "For you today" (red rows at the top), and the separate urgent
   card only shows when the urgent ones are nobody's. The getting-started card folds into the day card as one line
   once half the steps are done.
3. **Merge `:feature:update` into `:feature:settings`.** It is 145 lines; the banner stays where it is.

### U1. Interface fixes (visible, no change to the structure)

From the screenshots of release 0.145, every tab, light and Baianês.

1. **Undo** after Done, Approve and Delete: a snackbar "Done · Undo" for a few seconds, because a wrong tap (often a
   kid's) can't be taken back today. The rule for undoing lives in `:core:model` (going back to the chore as it was).
2. **A visible ⋮ on chore cards.** Remind, Who does it and Edit are only reachable by tapping the card or swiping, which
   few people find.
3. **One header for every tab**: a short title, one summary line, at most one action. Today Chores has "4 to do" with
   a black Add button, Groceries a big title plus a count, Family the family's name, Map and Settings none.
4. **Chores**: drop "5 on the grocery list" from its header (Casa already says it); shorter cards, with the avatars and
   points on the status line (about 2.5 cards fit on a screen today).
5. **Store screenshots in Portuguese with Portuguese content**: the pt-BR shots show "Hoover the living room",
   "Pharmacy", "Dinner's ready!". The store tests get Portuguese sample data.

### U2. More interface changes (the six tabs stay)

1. **One way to add**: a "+" in the same place on every tab, instead of today's five (black button on Chores, floating
   button on Rooms, menu on Casa, a button mid-page on Notes, a yellow button on Trips).
2. **Groceries**: the count shows twice ("5 to buy" and "To buy (5)") and "Feira" is both the title and the first list.
   The list chips become the header; "I'm at the shop" sits with the list.
3. **Map**: "Share my location" and "Arrival alerts" go together in one "This phone" card at the end of the Map
   (owner's choice, 18 November 2026: moving them to Settings would need the sharing logic in `:core:location`).
4. **TalkBack pass**: descriptions for every icon and button, checked with a screenshot test per tab.

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
| R4 | not doing | | |
| U1 | undo, ⋮ menu, one header style, shorter chore cards, store shots in Portuguese | low | 1 to 2 sessions |
| U2 | one "+", calmer Groceries, settings out of Map, TalkBack | medium | 1 to 2 sessions |
| R5 | none | low | 1 session |

Recommended order, agreed with the owner on 18 November 2026: **R1 and R2 first** (no visible change, smaller files to
work in), then **U1**, then **R3** (approved), **U2** and **R5**. R4 was turned down.
