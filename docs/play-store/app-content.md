# Other Play Console forms (answers)

## App access
The app needs a sign-in and a family, so give Google's reviewers a way in
(*Policy > App content > App access > Add instructions*):

1. Create a Google account only for the review (e.g. a new Gmail) and a second one if you want
   the reviewer to see two people.
2. Install the internal-test build with the first account and create a family named "Review family".
   Add a few chores and groceries so the screens aren't empty.
3. Write in the form: "Sign in with Google using the account below, tap Continue; the family
   'Review family' is already set up. Location sharing is off until switched on in the Map tab."
   plus the account's email and password.
4. Do not turn on two-step verification on that account, or the reviewer can't sign in.

## Ads
No ads.

## Content rating (IARC questionnaire)
Category: utility or reference. No violence, sexual content, profanity, drugs or gambling. Users
can exchange messages inside a family only (closed group; parents can delete messages). Shares
location with the family only after the person turns it on. Expect "Everyone" or the
equivalent.

## Target audience
Choose **18 and over**. Parents run the family; a child joins only if a parent approves, uses no
Google account, and sees no ads. Choosing an under-13 audience would put the app under Google's
Families policy, which is much stricter (for example on sign-in and third-party SDKs).

## News, COVID, government, financial features, health
All "No". "Take medicine" is only a chore title a family types; the app has no health features.

## Permissions to declare
- **Background location** (`ACCESS_BACKGROUND_LOCATION`) needs its own declaration, see below.
- Foreground location, calendar read and write, notifications, internet: no special form.
- `REQUEST_INSTALL_PACKAGES` is removed from the Play build (only the GitHub build has it).

## Background location declaration
**Feature:** Arrival alerts (and keeping a family member's position fresh on the family map).
**Why background:** When a family member arrives at or leaves a place the family saved (Home,
School), the family is notified. This must work with the app closed, otherwise nobody would be told
the child got home. Without background access the app falls back to updating only while open.
**User control:** Off by default. The person turns on "Share my location" in the Map tab, the app
explains why before Android's own prompt ("Allow all the time"), and they can switch it off at any
time; stopping removes their position from the family's data.
**Demo video** (30-60 s, upload to YouTube as unlisted and paste the link):
1. Map tab, Share my location off. Turn it on.
2. The app's explanation screen, then Android's "Allow all the time" prompt, then Allow.
3. Show the saved place "Home", close the app, and show the arrival notification (or the family
   map updating) from another phone.
4. Turn sharing off again.

If Google rejects it, tell me: I can remove background location from the Play build (arrival
alerts then only work with the app open) and resubmit.

## Developer contact
A public contact email or website is required. The project page works:
`https://github.com/ramonvidal/ChoreMe`; add your own email as well if you prefer.
