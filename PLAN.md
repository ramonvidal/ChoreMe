# ChoreMe plan

## Decisions so far

| Topic | Decision |
|---|---|
| Platform | Android only: Kotlin + Jetpack Compose |
| Structure | **As modular as possible**: `:core:*` and `:feature:*` Gradle modules, rules in `CLAUDE.md`, checked on every build |
| Sync | Firebase: Auth (Google sign-in) + Cloud Firestore (offline cache, real-time) + Cloud Messaging |
| UI language | English |
| Chores | **"Something needs doing"**: whoever notices it (kids too) adds it, and it's due now. Repeating is optional, folded away under "Repeat…". Adding something already open shows "already on the list" instead of a duplicate |
| Urgent chores | An **Urgent** switch when adding or editing a chore. Urgent ones go to the top of Today whatever their date, on a **red card** with an "URGENT" label, and the family gets a pop-up notification (its own "Urgent chores" channel). Red is kept for urgent only: overdue chores show "2 days late" in red text instead. Anyone can flag a new chore urgent; only parents change it later. It clears itself once the chore is done |
| Settings | No separate screen: the **Family** tab ends with **This phone**, folded rows for notifications, quiet hours, instant alerts and app updates |
| Agenda | The family calendar plus only **scheduled** chores: repeating ones and one-offs planned for a later day. Everyday "needs doing" chores stay in the Chores tab |
| Family news | **"I'm at the shop"** (turning on shopping mode) and **"On my way home"** (Map tab): one tap, shown on the others' screens and as a notification on phones with ntfy. Each lasts a while (2 h, 1 h) and can be taken back |
| Reminders at a time | "Remind me" on a chore: a notification at that time on **this phone only**; skipped if the chore is done by then |
| More lists | Besides groceries: pharmacy, hardware store, packing... Same list behaviour, picked at the top of the Groceries tab. Anyone makes one; its maker or a parent removes it |
| Family messages | "Message everyone" at the top of the Family tab: one line ("Dinner's ready!") that pops up on every phone (instantly with ntfy, otherwise within about 30 minutes once that phone has opened the Family tab). Kept a week; the sender or a parent removes it |
| Notes board | Short notes on the Family tab everyone sees. The writer or a parent edits them |
| This week | Parents see the points each person earned in the last 7 days. Each chore is worth 1 to 5 points, set by a parent when creating or editing it and kept on the chore (and its saved quick pick) for every repeat; kids' reports start at 1. The count includes chores finished before the log existed (their last finish) and every finish, even twice a day |
| Repeating | Once, every day, every few days, on certain weekdays, or every month on a day (the 29th to 31st fall on the last day of shorter months). A repeat can end on a date ("until 31 Dec 2027"); finishing the last time round finishes it for good |
| Adding a chore | Typing first: the title box filters the ready-made and saved chores as you type (by name, or by a word for the kind of chore in English, Portuguese or German). Before typing: the family's 6 most used, then groups folded closed (Kitchen, Cleaning, Laundry, Bins, Care, Errands, Your own). A typed chore remembers the ready-made one it's closest to, for its group and icon |
| Together | A parent can give a chore to someone (or several) when creating or editing it ("For" in the editor); they get "For you: …" and it shows under Yours. The assignment stays: a repeating chore goes back to the same people every time round. Yours shows only what's due by today (or urgent); later ones wait under Coming up. Several people can do a chore together: "Me too" joins whoever is on it, and parents can pick who's doing it or who did it (several at once). Everyone on it gets the chore's full points in their own week; the family's total and the garden count it once. Kids only ever add or remove themselves (firestore.rules) |
| Yours | Chores you said you'd do ("I'll do it") sit in their own "Yours" section at the top of the Chores tab, on a green-edged card, whatever their day; the header says "2 are yours" and the morning notification starts with "You're on 2" |
| Our home | 8-bit pixel art (big square pixels, a flat palette, animations stepping 8 frames a second): a small house and garden at the top of the Chores tab, shared by the whole family (cooperative, not a ranking): it changes with every chore: each chore to do today is a weed in the grass, and finishing it pulls the weed and grows flowers (one per point). Growth counts the last 7 days' points, rolling, so there's no weekly reset (full bloom at 30), the lights are on while nothing is overdue, the weather worsens as chores wait (chores to do today, plus overdue and urgent ones again: clouds from 3, rain from 6, a storm with lightning from 10; anything urgent brings a cloud), overdue chores' weeds are overgrown, and the flowers droop (never anything sad) from 3 overdue chores. Tap for everyone's points |
| Week in review | Sunday at 19:00 each phone posts "This week: 23 points" with each person's points. Can be turned off under This phone |
| Away | Anyone marks themselves away until a date (parents can mark anyone): no chore alerts on their phone, not offered as "done by" |
| Tab names | Chores, Groceries, Agenda, Map, Family |
| Chore assignment | **Whoever grabs it**: chores are open to everyone, and one tap claims one |
| Kids | Kids are members with the `KID` role. Chores they complete go to a parent for **approval** before counting as done; a rejected chore goes back to the same kid |
| Kids' sign-in | Kids **don't need a Google account**: a parent creates a kid profile, and a kid's device joins it with a parent's approval (see below) |
| Rewards | None for now: **parent approval is enough** |
| Calendar | Use the family's **existing Google Calendar** through the calendars already synced on each phone (Android `CalendarContract`), not the Google Calendar API: no Google Cloud setup, works offline, and Google sends the alerts. Each phone picks which calendars to show |
| Location | **Opt-in per person**, pause any time (stopping deletes the shared position). Everyone in the family, kids included, sees who shares. Updates about every 15 minutes in the background and when the map opens. **OpenStreetMap** (no API key). Named places (Home, School) set by parents. No instant arrive/leave alerts on other phones until server push |
| Cost | **Everything free**: Firebase Spark plan, OpenStreetMap, the phone's own calendar, GitHub Actions, and UnifiedPush through the free ntfy app for instant pings. No paid plan, no server of our own |
| Notifications | Each phone posts a **morning summary** (WorkManager) and the app shows badges. **Instant pings** between phones go through UnifiedPush: each phone with the free **ntfy** app installed gets a contentless wake-up ping and reads the details from Firestore. Phones without it fall back to checking every ~15 minutes |
| Look | **"Soft cards"**: a light grey page, white cards, black and grey text, and **one green** only for the main action and the selected tab; red only for urgent. Font: Figtree. Simple on purpose: chosen over warmer, more colorful options |
| Colour rules | A green edge means a chore is yours, a red edge that it's urgent; chore buttons stay plain. People show as their coloured avatar. Swipe a chore right for its button, left for its options; swipe a grocery to tick it off. Finishing a chore shows "Well done! +2 points" with a tick and a buzz |
| Screenshots | Every tab (light, dark, the largest text size, empty) is drawn from its state alone (`XxxContent`) and screenshotted on each release (`screenshots.zip`) |
| Pictures | **No emoji.** Chores and grocery items use line drawings from **Tabler Icons** (MIT); app controls use Google's **Material Symbols** (Apache 2.0). Only the drawings in use are copied into `:core:ui`, so nothing is downloaded and they follow light/dark mode |
| Offline | Firestore's built-in offline cache; no separate local database. Writes show up at once and sync when there's signal. A write the server refuses (or that fails) shows "Couldn't save that" at the bottom of the app (`WriteFailures` in `:core:data`). CI publishes the tested rules to Firebase on every push (`FIREBASE_SERVICE_ACCOUNT` secret) |

## Sharing and sync

A **family** is the unit of sharing. One parent creates it and gets an invite code
to share. Anyone else, parent or kid, signs in, enters the code and **asks to
join**; a parent approves them from the Family tab (a badge shows when someone is
waiting). The code alone never gives access, so a leaked code is harmless.

Access is decided only by `memberUids/{uid}`, which only a parent can write (or
the creator, for themselves, when creating the family). Kids can't approve
requests, change roles or remove members. These rules live in
`firebase/firestore.rules` and are tested against the Firestore emulator in CI.

```
families/{familyId}              name, inviteCode, createdBy
  memberUids/{uid}               memberId, role: the access list, one entry per signed-in device
  members/{memberId}             name, role (PARENT | KID), color, uids[]
  joinRequests/{uid}             name, role, deviceName, createdAt
  groceryItems/{itemId}          name, category, isNeeded, isStaple, updatedAt, updatedBy
  locations/{memberId}           latitude, longitude, accuracy, at: only while that member shares
  places/{placeId}               name, latitude, longitude, radius
  placeEvents/{eventId}          memberId, placeId, placeName, type (ARRIVED | LEFT), at; kept 2 days
  pushEndpoints/{uid}            memberId, url: where to ping this device (UnifiedPush)
  choreTemplates/{nameKey}       title, createdBy: hand-typed chores kept as quick picks
  chores/{choreId}               createdAt, title, template, recurrenceType, everyDays, weekDays[], dueDate,
                                 status, claimedBy, completedBy, approvedBy, lastDoneBy, lastDoneOn, notifyWhenDone
inviteCodes/{code}               familyId, familyName
users/{uid}                      familyId, familyName: lets a device find its family again
```

### Kids without a Google account

1. On the kid's phone or tablet, they tap **"I'm a kid"**, type their name and the
   invite code. The device signs in with Firebase **anonymous auth**, so no email or
   Google account is needed. Anonymous devices can only ask to join as a kid.
2. A parent **approves it on their own phone** and picks which kid it belongs to:
   an existing kid profile or a new one. A kid can have several devices.
3. A parent can remove a kid (and so all their devices) at any time. A kid who
   signs out needs approval again, since anonymous accounts can't sign back in.
4. Parents can also add a **kid profile without a device** from the Family tab.

The parent's approval is the security check, so this works without Cloud
Functions and the project stays on the free Spark plan. Kids without any device
still get chores: a parent can mark a chore as done on their behalf.

Chore states: `OPEN → CLAIMED → (kid) PENDING_APPROVAL → done`; parents' work counts
straight away. "Done" means `DONE` for one-off chores; recurring chores go back to
`OPEN` on their next date, counted from the later of the due date and the day it was
done. The rules live in `core/model/.../chore/` and are unit tested; the Firestore
rules let kids make only the grab / let go / "I finished" steps, in their own name.

## Phases

- [x] **Phase 0: Skeleton.** Compose app with bottom navigation (Today, Groceries,
      Agenda, Family), Hilt, Firebase wiring, chore approval rules with tests,
      GitHub Actions building the APK.
- [x] **Phase 1: Family and accounts.** Google sign-in for parents, anonymous
      sign-in for kids, create a family, ask to join by invite code, parent
      approval of every join, members list, kid profiles with or without a device,
      Firestore security rules with emulator tests.
- [x] **Phase 2: Groceries.** One catalog per family where items move between
      "at home" and "to buy": tap what ran out, tick it off in the store (with
      undo). Quick add with autocomplete that never duplicates an item, automatic
      aisle sorting in store order, starred "usuals" with one-tap restock, a
      starter list for new families, and offline-first sync. Item ids come from
      the name, so two phones adding "Milk" offline end up with one item.
- [x] **Phase 3: Chores.** Templates (dishes, laundry, fold clothes, kitchen,
      bathroom, trash, vacuum, beds, plants, groceries) or anything typed;
      repeat once, every N days or on chosen weekdays. Today tab with "waiting
      for your OK", to do (late ones highlighted), coming up and done today.
      Anyone grabs a chore; kids' finished chores go to a parent to approve or
      send back; parents can record a chore done by a kid without a phone.
      Morning reminder on each phone, badge on Today for approvals, and a link
      to the grocery list when it has items.
- [x] **Phase 4: Agenda.** The next 14 days, day by day: events from the chosen
      calendars (family calendar preselected by name; repeating events included)
      next to the chores due. Tap an event to open it in the calendar app. Quick
      "Add event" with day, time, length and an alert (all-day events alert the
      evening before). Calendar access lives in its own `:core:calendar` module.
      Kids without a Google account on their phone see the chores only.
- [x] **Phase 5: Location.** Map tab with a dot per member who shares, and a
      list saying "At School · 10 min ago" or "Away from saved places". Sharing
      is switched on by each person, asks for "allow all the time" for
      background updates, and can be turned off any time. Parents save places
      where they stand, with a size of 100, 250 or 500 m.
- [x] **Feedback round 1.** Chores reframed as "noticed needs" (anyone reports,
      no schedule by default, duplicates caught). Grocery items get an emoji
      each (🥛 🥚 🍞 🧻 …), falling back to their aisle's.
- [x] **Feedback round 2.** "Save this place" button on the map with a clear
      result message. **Arrival alerts**: the phone that moves records "Leo
      arrived at School" / "Leo left Home" (`placeEvents`), with a margin so GPS
      jitter at the edge doesn't flap; every phone checks about every 15 minutes
      and shows a notification. Each phone can turn alerts off.
- [x] **Instant alerts, free.** New `:core:push` module (UnifiedPush). Phones
      with the free ntfy app get pinged the moment someone arrives or leaves,
      and show the alert within seconds. Pings carry only the kind of change;
      addresses live in `pushEndpoints`. Next candidates for pings: kids'
      finished chores (to parents) and join requests.
- [x] **Feedback round 3.** "Leo noticed: Wash dishes" notifications on the
      other phones (instant with ntfy, otherwise within 30 minutes). Hand-typed
      chores are saved as quick picks ("Saved" row; long-press to remove).
      Battery: no background Firestore listeners, location only sent when moved
      (or every 30 min), recent fixes reused, slower safety-net checks when
      pings work. Agenda gets a **month view** (dots per event in its
      calendar's color and for chores; tap a day for its list), next to the
      two-week list.
- [x] **Feedback round 4.** Instant pings to parents (kids' finished chores, join
      requests). Leave family / delete my data. Shopping mode. Chore → calendar
      event. Per-phone settings (which alerts, quiet hours). Home-screen widget.
- [x] **"Let everyone know when it's done"** per chore (on by default): the rest of
      the family gets "✅ Leo did: Wash dishes" when it's finished (for kids, once
      approved). Each phone can switch these off in its settings.
- [x] **Updates from inside the app.** Once a day ChoreMe checks the release page;
      a newer build shows "ChoreMe 0.31 is available – Install", downloads, and
      Android asks to confirm. No store or extra app needed (only the very first
      install is from the link). Modules `:core:update` and `:feature:update`.
- [ ] **Extras.** Home-screen widget ("Out of…" and today's chores), weekly
      dark mode polish.

## Open questions

- None right now. To try Phase 1 on a phone, set up Firebase (see README).
