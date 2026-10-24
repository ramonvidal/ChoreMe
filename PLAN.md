# ChoreMe plan

## Decisions so far

| Topic | Decision |
|---|---|
| Platform | Android only: Kotlin + Jetpack Compose |
| Structure | **As modular as possible**: `:core:*` and `:feature:*` Gradle modules, rules in `CLAUDE.md`, checked on every build |
| Sync | Firebase: Auth (Google sign-in) + Cloud Firestore (offline cache, real-time) + Cloud Messaging |
| UI language | English |
| Chore assignment | **Whoever grabs it**: chores are open to everyone, and one tap claims one |
| Kids | Kids are members with the `KID` role. Chores they complete go to a parent for **approval** before counting as done; a rejected chore goes back to the same kid |
| Kids' sign-in | Kids **don't need a Google account**: a parent creates a kid profile, and a kid's device joins it with a parent's approval (see below) |
| Rewards | None for now: **parent approval is enough** |
| Calendar | Use the family's **existing Google Calendar** (user picks it from their calendar list); Google sends the alerts |
| Location | Deferred to the last phase |
| Offline | Firestore's built-in offline cache; no separate local database. Writes show up at once and sync when there's signal |

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
  chores/{choreId}               (Phase 3) title, icon, recurrence, status, claimedBy, completedBy, approvedBy
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

Chore states: `OPEN → CLAIMED → (kid) PENDING_APPROVAL → DONE`; parents go straight
to `DONE`. The rule lives in `domain/model/Chore.kt` and is unit tested.

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
- [ ] **Phase 3: Chores.** Templates (laundry, dishes, kitchen, bathroom, fold
      clothes, …), recurrence, grab/complete, parent approval queue, push
      notifications for overdue chores and approvals. A "Go grocery shopping"
      chore appears when the list grows.
- [ ] **Phase 4: Agenda.** Pick the existing family Google Calendar, show its events
      with chores that are due, add events from the app.
- [ ] **Phase 5: Location.** Opt-in live family map and arrive/leave alerts.
- [ ] **Extras.** Home-screen widget ("Out of…" and today's chores), weekly
      scoreboard or points for kids (not needed for now), dark mode polish.

## Open questions

- None right now. To try Phase 1 on a phone, set up Firebase (see README).
