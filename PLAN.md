# ChoreMe plan

## Decisions so far

| Topic | Decision |
|---|---|
| Platform | Android only: Kotlin + Jetpack Compose |
| Sync | Firebase: Auth (Google sign-in) + Cloud Firestore (offline cache, real-time) + Cloud Messaging |
| UI language | English |
| Chore assignment | **Whoever grabs it**: chores are open to everyone, and one tap claims one |
| Kids | Kids are members with the `KID` role. Chores they complete go to a parent for **approval** before counting as done; a rejected chore goes back to the same kid |
| Kids' sign-in | Kids **don't need a Google account**: a parent creates a kid profile, and a kid's device joins it with a parent's approval (see below) |
| Rewards | None for now: **parent approval is enough** |
| Calendar | Use the family's **existing Google Calendar** (user picks it from their calendar list); Google sends the alerts |
| Location | Deferred to the last phase |

## Sharing and sync

A **family** is the unit of sharing. One parent creates it and gets an invite code
(also as a QR code or share link); others sign in and enter the code to join.
Firestore security rules allow only members to read or write a family's data, and
only parents to approve chores or manage members.

```
families/{familyId}              name, inviteCode, createdBy
  members/{memberId}             name, role (PARENT | KID), color, deviceUids[]
  joinRequests/{uid}             kid member requested, deviceName, createdAt
  chores/{choreId}               title, icon, recurrence, status, claimedBy, completedBy, approvedBy
  groceryItems/{itemId}          name, category, isNeeded, isStaple, updatedAt
```

### Kids without a Google account

1. A parent adds a **kid profile** (name, color) in the Family tab.
2. On the kid's phone or tablet, the app is opened with **"I'm joining as a kid"**,
   the family invite code is entered and the kid picks their profile. The device
   signs in with Firebase **anonymous auth**, so no email or Google account is needed.
3. This creates a join request that a parent **approves on their own phone**.
   Only then do the security rules link that device to the kid profile.
4. A kid can have several devices, and a parent can remove one at any time.

The parent's approval is the security check, so this works without Cloud
Functions and the project stays on the free Spark plan. Kids without any device
still get chores: a parent can mark a chore as done on their behalf.

Chore states: `OPEN → CLAIMED → (kid) PENDING_APPROVAL → DONE`; parents go straight
to `DONE`. The rule lives in `domain/model/Chore.kt` and is unit tested.

## Phases

- [x] **Phase 0: Skeleton.** Compose app with bottom navigation (Today, Groceries,
      Agenda, Family), Hilt, Room, Firebase wiring, chore approval rules with tests,
      GitHub Actions building the APK.
- [ ] **Phase 1: Family and accounts.** Google sign-in for parents, create or join
      a family by invite code, member list, parent/kid roles, kid profiles joined
      by approved devices, Firestore security rules.
- [ ] **Phase 2: Groceries.** Staples catalog grouped by category; tap to mark
      "out of", tap again in the store to mark bought; "weekly restock" button;
      quick add with autocomplete; live sync.
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

- None right now. Phase 1 needs the Firebase project (see README).
