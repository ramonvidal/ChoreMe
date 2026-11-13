# A "Casa" tab and fewer tabs

Status: **draft for discussion**, nothing is built. Written 13 November 2026, from the owner's idea: a **Casa** tab with the
day's summary, the agenda, the grocery count and the weather with the little house at the top; **Rooms** becomes a
sub-tab of **Chores** and stops being a tab of its own.

## 1. Where the app is today

Six bottom tabs: **Chores, Groceries, Rooms, Agenda, Map, Family**. Chores opens first and carries the yellow hero card
(date, what needs doing, grocery count, "Something needs doing"), the little weather house, the room filter and the chore
list. The week's trips live under Family. Six tabs is already the most Material allows without crowding at large text.

## 2. The proposal

Five tabs: **Casa, Corres, Feira, Mapa, Família.**

| Tab | What is in it |
|---|---|
| **Casa** (first, new) | The little house with the real weather at the very top. Under it the yellow day card: date, "N chores, N events, N items to buy", and one button. Then **Today**: one timeline of the day (calendar events, chores due, the family's trips with their weather). Sub-tabs **Hoje / Agenda** (and later **Semana**). |
| **Corres** | Sub-tabs **Corres / Cômodos**. Corres is today's chore list (board, filters, #rooms). Cômodos is the Rooms screen as it is now. |
| **Feira** | Unchanged (grocery lists). |
| **Mapa** | Unchanged. |
| **Família** | Unchanged, except the weekly plan may move (see 5). |

Agenda stops being a tab: its list and month view become the **Agenda** sub-tab of Casa. Rooms stops being a tab: it becomes
a sub-tab of Corres.

## 3. What this means for the code (the part to decide on purpose)

CLAUDE.md says features never depend on each other and talk only through `:app`. Two things in the proposal touch that rule.

1. **Casa needs data from several areas.** It can read it from the core modules (`ChoreRepository`, `GroceryRepository`,
   `CalendarRepository`, `PlanRepository`, `WeatherRepository`, the board and agenda rules in `:core:model`), so a new
   **`:feature:home`** module is allowed without breaking anything. It shows summary lines, not the full chore cards (those
   stay in `:feature:chores`). The weather house and its view model move from `:feature:chores` to `:feature:home`.
2. **Two features in one tab (Corres + Cômodos, Hoje + Agenda).** A tab with sub-tabs has to show one screen from
   `:feature:chores` and one from `:feature:rooms`, and `:feature:home` would host `:feature:agenda`'s screens. A feature
   cannot depend on another, so the sub-tab host has to live in `:app`, and **each feature must expose a public composable
   entry point besides its navigation entry** (like `UpdateBanner()` today). That is a new, documented **exception** to
   "each feature exposes one entry point". Alternative: merge Rooms into `:feature:chores` and Agenda into `:feature:home`
   (bigger modules, no exception). I recommend the exception: modules stay small, the rule changes by one line in CLAUDE.md
   and in `ModuleRules`' tests.

Other consequences:

- `onOpenGroceries` and the deep links (`choreme://groceries`) keep working; the widget's targets are unchanged.
- The tab badges move: Corres keeps "needs you", Feira keeps "to buy", Casa has none (the numbers are in its card).
- The start destination becomes Casa; notifications open Casa (weather, morning summary) or Corres (chores) as now.
- Every tab screenshot, the Play Store screenshots and the feature graphic change; they are redone at the end.
- Tests: `TodayScreen`/`ChoresContent` lose the weather card and gain a sub-tab header; new tests for Casa's timeline logic,
  which goes in `:core:model` (plain Kotlin, tested on the JVM).

## 4. Casa's "Today" timeline (the new logic)

Pure Kotlin in `:core:model`: `dayTimeline(date, events, chores, trips, forecasts)` returns the items of the day in time
order:

- calendar events (time, title, place),
- the family's trips with their weather advice ("8:00 Work, Ramon: rain 80%, rain jacket"),
- chores due today without a time, grouped at the top ("3 chores need doing") so they do not bury the day,
- one line for the groceries ("5 items to buy") with a button to the Feira tab.

Quiet trips stay short ("8:00 Work: all good").

## 5. Suggestions beyond the idea

1. **Casa opens the app** and shows the answer to "what is today like?" in one glance. The yellow card keeps one button: the
   most useful one (add a chore for a grown-up on a weekday morning; "I'm in the shop" when someone is shopping).
2. **A "Semana" sub-tab in Casa**: the weekly plan, a 7-day strip with a weather symbol per day and each trip under its day.
   The plan editor moves here from Family ("Your week") where people will look for it.
3. **Today's trips as the weather's context**: tapping a trip opens the hour-by-hour weather for it.
4. **First-run checklist on Casa** until done: "Save Home on the map", "Add a trip", "Allow notifications". It replaces the
   plain "Save your home" line.
5. **The points ranking stays on Family** (it is about the family, not the day), and Casa's card shows nothing about points.
6. **Pull the grocery count into the card as a tappable chip** ("5 to buy") that opens Feira; no extra tab needed to see it.
7. **Keep Agenda's "+ event" button** inside the Agenda sub-tab, and offer "add to calendar" for a chore from Casa as today.
8. **Widget**: keep as is (chores and "out of...") and add one weather line; it already has a free line.
9. **Large text**: sub-tab headers wrap into a scrollable row; checked at 1.5x.

## 6. Questions for the owner

1. **Agenda**: a sub-tab of Casa (my proposal), or its own place inside Corres, or still a tab?
2. **Casa's sub-tabs**: Hoje / Agenda, and later Semana. Right set?
3. **The yellow card's button**: keep "Something needs doing" there (and on Corres), or give Casa a different one?
4. **Code rule**: OK to add the exception (features expose a public composable for tabs that host sub-tabs), or do you
   prefer merging Rooms into Chores and Agenda into Casa?
5. **Names**: "Casa", "Corres", "Cômodos", "Feira", "Mapa", "Família" in Baianês; in English "Home", "Chores", "Rooms",
   "Groceries", "Map", "Family". OK?
6. **Where the weekly plan lives**: Family (as now) or Casa's Semana sub-tab?

## 7. Phases

| Phase | What | Done when |
|---|---|---|
| H0 | Answers to section 6 | agreed |
| H1 | `:feature:home` with the house, the yellow day card and the Today timeline (logic in `:core:model` with tests) | Casa shows the day |
| H2 | Sub-tab host in `:app`; Agenda becomes Casa's Agenda sub-tab; entry points and CLAUDE.md exception | Agenda tab gone |
| H3 | Corres with the Cômodos sub-tab; Rooms tab gone; badges and start destination | five tabs |
| H4 | Semana sub-tab and the plan editor moved | optional |
| H5 | Widget line, deep links, notifications' targets, screenshots and Play assets redone | CI green, owner tests |

Each phase ends with CI green and the screenshots looked at before it is called done.

## 8. Risks

- **Navigation regressions**: deep links, notification taps and the widget must still land on the right screen; each is
  checked on its own.
- **Sub-tab state**: each sub-tab keeps its scroll position and filters while switching (saved with the nav state).
- **Casa becoming a dumping ground**: only what the day needs; details stay in their own tabs.
- **Size of the change**: it touches `:app`, three features and every tab screenshot, so it goes in the phases above, never in one push.
