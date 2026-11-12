# Nos Corre!: visual redesign plan ("Sunny", from BoraTreino)

Goal: give Nos Corre! the same look as the owner's other app, **Bora pro Treino!** (repo `ramonvidal/BoraTreino`):
a warm off-white page, plain white cards, a yellow brand colour, charcoal ink, Figtree type, one card style, three
button levels. This file is the plan; nothing here is built yet. Section 5 lists the questions that need the owner's
answer before the first mock-up.

Sources read (read-only): `core/ui/.../theme/Theme.kt`, `BoraCard.kt`, `BoraButton.kt`, `BoraChip.kt`, `Mascot.kt`,
the nav bar in `BoraTreinoNavHost.kt`, `plan_redesign.md` and `docs/redesign/mockups.png` of BoraTreino.

## 1. What the BoraTreino look is (taken from its code)

| Part | BoraTreino | Value |
|---|---|---|
| Brand | yellow (selected things, hero cards, tab indicator) | `#FEC204`, soft `#FFF1B8` |
| Ink | charcoal: text and the one filled button | `#1F1B16` |
| Page / cards | warm off-white / white | `#FAF7F0` / `#FFFFFF`; tile `#F3EEE3` |
| Success | green, only for done / records / positive change | `#2E6B52`, soft `#DDEBE4` |
| Accent | pink, sparingly (celebration, one badge) | `#E6397F` |
| Muted text | warm grey, darker than before so it reads | `#6B645A` |
| Error | red, only errors | `#B42318` |
| Type | Figtree; four sizes: 32 numbers/titles, 20 titles, 16 body, 13 labels | same font file Nos Corre! already bundles |
| Shapes | 8 / 12 / 16 / 20 / 28 dp | cards use 20 dp |
| Card | ONE style: white, 20 dp, no border, no shadow, 16 x 14 dp padding; lists live in one card with hairline dividers | `BoraCard` |
| Buttons | three levels: **filled** (charcoal, white text, 56 dp, one per screen), **tonal** (soft yellow, charcoal text), **text** | `BoraButton` |
| Chips | selected = filled yellow, no check mark; unselected = sand tile, no outline | `BoraChip` |
| Bottom bar | white, no elevation; selected tab = charcoal icon and label on a **yellow rounded indicator** | |
| Hero card | one big yellow card at the top of the main tab: date small, title large, progress ring, ONE charcoal button | Today screen |
| Mascot | a character in empty states, rest day, first run, "done" | `Mascot(sex)` |
| Dark mode | none: the app stays light-only (owner's choice) | |

## 2. Where Nos Corre! differs today ("Soft cards")

| Part | Nos Corre! now | Becomes |
|---|---|---|
| Page | cool grey `#F4F4F2` | warm `#FAF7F0` |
| Primary | **green** `#2E6B52` (main action, selected tab, "yours" edge) | charcoal ink for actions, **yellow** for selected; green stays for success / "yours" |
| Selected tab | green icon and text, no indicator | charcoal on a yellow rounded indicator |
| Main action | green button, black "+" FAB | one filled charcoal button per screen; FAB charcoal (already) |
| Chips / tabs | `PillTab`: black when selected, white otherwise | yellow when selected, sand tile otherwise |
| Cards | white cards, mixed corner sizes, some outlines (green "yours", red urgent) | one 20 dp card style; the two edges stay because they carry meaning |
| Header | big title + grey line | same, plus a yellow **hero card** on Chores (see 4) |
| Dark mode | follows the phone | **open question** (section 5) |
| Colours per person | 8 member colours, white initial | unchanged; checked against yellow for contrast |
| Illustrations | room plans, the pixel-art house, a small sunny-house empty state | kept; empty states get the family's own mascot if the owner wants one |

## 3. Design system work (`:core:ui`, phase R1)

Nothing is copied as code: the two apps stay independent (their modules and packages differ). The same values and
component shapes are re-created in Nos Corre!'s own `:core:ui`, so every tab picks them up.

1. `theme/Theme.kt`: `Sunny` palette object, light colour scheme (primary = ink, primaryContainer = yellow, secondary =
   green, tertiary = pink), shapes 8 to 28 dp, type scale as above. Keep Figtree.
2. New components, each with a unit/screenshot test:
   - `NosCard` (one card style, optional tint, optional click),
   - `NosButton` (Filled / Tonal / Text),
   - `NosChip` (replaces `PillTab` and the filter chips),
   - `HeroCard` (yellow card with title, line, optional progress ring and one button),
   - `SectionLabel` in small caps style, `RoomTag` and the filter row restyled.
3. Bottom bar: yellow indicator, six tabs (Chores, Groceries, Rooms, Agenda, Map, Family) checked for fit at large text.
4. Window background colour in `app` and `themes.xml` to the new page colour (no flash on start).
5. Launcher icon: the current background `#FDDC03` is already close to the brand yellow `#FEC204`; align it.

## 4. Screen by screen (what changes)

- **Chores**: yellow hero card replacing the plain header block: date, "N need doing", a progress ring for "done today
  out of today's chores" and one charcoal button ("Add a chore" for parents, "Grab one" for kids). The living-house
  picture stays, moved under the hero as a normal card. Chore cards: one style, hairline-divided groups,
  "Done / I'll do it" as small tonal buttons; red urgent and green yours edges stay.
- **Groceries**: one grouped white card (already), list picker as `NosChip`s, "Restock usuals" as a tonal button.
- **Rooms**: room cards keep the floor-plan picture on a sand tile; yellow `#rooms` tag and filter chips.
- **Agenda**: list/month and the room filter as `NosChip`s; the day cards as `NosCard`.
- **Map**: sharing switch row as a card; the "allow all the time" notice as a tonal card.
- **Family**: member rows with avatars in one card, section labels in small caps, settings as rows.
- **Onboarding and sign-in**: yellow hero with the app icon, one charcoal button.
- **Widget**: page colour and ink; the yellow only as the title accent (Glance themes are limited).
- **Play Store assets**: screenshots and feature graphics redone in the new look (last, phase R6).

## 5. Decisions (answered by the owner, 12 November 2026)

1. Dark mode **stays**, with a setting under Family > This phone > Look: same as the phone / light / dark.
2. The main action is charcoal (yellow in the dark look); green stays for "yours", "done" and points.
3. The mascot is the **house of the launcher icon** (`Mascot()` in `:core:ui`).
4. Chores gets the yellow hero card with the progress ring and one button (it replaces the + button).
5. The icon stays as it is. Its art is lemon yellow (`#FDE003`), the brand yellow is amber (`#FEC204`); changing the
   launcher background alone would show a seam, so it is left alone.
6. **All tabs**, in phases R1 to R6.

The questions as asked (kept for the record):

1. **Dark mode.** BoraTreino is light-only. Nos Corre! follows the phone today. Keep dark mode (needs a dark Sunny
   palette: yellow stays, page `#121212`) or become light-only like BoraTreino? Recommendation: keep dark mode; it costs
   one extra palette and the screenshots already cover it.
2. **Green.** In Nos Corre! green currently means "yours" and "main action". In Sunny, green means success only. OK to
   move the main action to charcoal/yellow and keep green for "yours", "done" and points?
3. **Mascot.** BoraTreino has a man and a woman (from its icon). Nos Corre! has no character. Options: none (use the
   yellow house of the icon for empty states), or ask for one family illustration to use like `Mascot`. Recommendation:
   none for now; reuse the icon art.
4. **Hero card on Chores.** Yellow hero with progress ring and one button, as on BoraTreino's Today. Wanted?
5. **Icon.** Keep the current icon, with its background aligned to `#FEC204`?
6. **Scope.** All tabs in one go, or Chores + Groceries + Rooms first, the rest after you tested?

## 6. Phases

| Phase | What | Done when |
|---|---|---|
| R0 | Static mock-ups of Chores, Rooms, Groceries and Family in the new look (rendered images), plus answers to section 5 | owner approves the images |
| R1 | Design system in `:core:ui` (section 3), no screen redone yet | every screenshot changes colour; tests green |
| R2 | Chores tab: hero card, cards, buttons, chips | owner tests on the phone |
| R3 | Groceries and Rooms | |
| R4 | Agenda and Map | |
| R5 | Family, onboarding, widget | |
| R6 | Wording and motion pass (EN, Baianês), Play Store screenshots and graphics redone | |

Each phase follows CLAUDE.md: logic in `:core:model`, strings per module (EN and Baianês), CI green, and the generated
screenshots looked at before reporting. The owner tests between phases.

## 7. Risks

- Yellow has low contrast with white: it never carries white text (charcoal on yellow everywhere). Member avatars and
  the yellow indicator need a contrast check.
- Six bottom tabs with a yellow indicator are tight at large text; check at 1.5x.
- Every screenshot test changes at once (they only record images, so this is noise, not breakage).
- Dark mode doubles the palette work if kept.
- Store assets (screenshots, feature graphics, icon background) must be redone after the Play listing exists; do it
  before the first public release.
