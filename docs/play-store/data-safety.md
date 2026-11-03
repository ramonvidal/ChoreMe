# Data safety form (answers)

Play Console, *Policy > App content > Data safety*. Answers for the Play build. Check each against
the screen as you go: Google's wording changes. Firebase (Authentication, Firestore) acts as the
app's service provider, which Google does not count as "sharing".

**Does the app collect or share required user data types?** Yes.
**Is all data encrypted in transit?** Yes (Firebase uses TLS).
**Can people request that their data be deleted?** Yes: in the app (Family, Your data) and on the
web page `https://ramonvidal.github.io/ChoreMe/delete-account.html`.
**Independent security review / Families policy:** No.

| Type | Collected | Shared | Optional | Purpose |
|---|---|---|---|---|
| Personal info: Name | Yes | No | No | App functionality, account management |
| Personal info: Email address | Yes | No | No | Account management (Google sign-in; kids' anonymous sign-in has none) |
| Personal info: User IDs | Yes | No | No | App functionality, account management |
| Location: Precise location | Yes | No | **Yes** (off until the person turns on sharing) | App functionality |
| Location: Approximate location | Yes | No | Yes | App functionality (same data) |
| Messages: Other in-app messages | Yes | No | Yes | App functionality (family messages and notes) |
| App activity: Other user-generated content | Yes | No | No | App functionality (chores, groceries) |
| Device or other IDs | Yes | No | No | App functionality (Firebase installation ID, notification address for ntfy) |

Not collected: financial info, health and fitness, contacts, photos and videos, audio, files,
web browsing, app interactions for analytics, crash logs, diagnostics. Calendar events are read
and written on the phone only and are not sent to the app's servers, so they are not "collected".
