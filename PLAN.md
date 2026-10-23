# ChoreMe plan

## Decisions so far

| Topic | Decision |
|---|---|
| Platform | Android only: Kotlin + Jetpack Compose |
| Sync | Firebase: Auth (Google sign-in) + Cloud Firestore (offline cache, real-time) + Cloud Messaging |
| UI language | English |
| Chore assignment | **Whoever grabs it**: chores are open to everyone, and one tap claims one |
| Kids | Kids are members with the `KID` role. Chores they complete go to a parent for **approval** before counting as done; a rejected chore goes back to the same kid |
| Calendar | Use the family's **existing Google Calendar** (user picks it from their calendar list); Google sends the alerts |
| Location | Deferred to the last phase |

## Sharing and sync

A **family** is the unit of sharing. One parent creates it and gets an invite code
(also as a QR code or share link); others sign in and enter the code to join.
Firestore security rules allow only members to read or write a family's data, and
only parents to approve chores or manage members.

```
families/{familyId}              name, inviteCode, createdBy
  members/{userId}               name, role (PARENT | KID), color
  chores/{choreId}               title, icon, recurrence, status, claimedBy, completedBy, approvedBy
  groceryItems/{itemId}          name, category, isNeeded, isStaple, updatedAt
```

Chore states: `OPEN → CLAIMED → (kid) PENDING_APPROVAL → DONE`; parents go straight
to `DONE`. The rule lives in `domain/model/Chore.kt` and is unit tested.

## Phases

- [x] **Phase 0: Skeleton.** Compose app with bottom navigation (Today, Groceries,
      Agenda, Family), Hilt, Room, Firebase wiring, chore approval rules with tests,
      GitHub Actions building the APK.
- [ ] **Phase 1: Family and accounts.** Google sign-in, create or join a family by
      invite code, member list, parent/kid roles, Firestore security rules.
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
      scoreboard for kids, dark mode polish.

## Open questions

- **Kids' sign-in.** Do the kids have their own Google accounts (e.g. through
  Family Link)? If not, Phase 1 can add a "kid profile" that a parent creates and
  that signs in with a short PIN on a shared or kid's device.
- **Points/rewards for kids.** Do you want a weekly scoreboard or points, or just approval?
