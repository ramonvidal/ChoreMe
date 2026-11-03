# Publishing on Google Play

App ID on Play: `com.punyplatypus.noscorre` (Play Console app already created).
Two builds come from the same code: **github** (the APK on the release page, updates itself) and
**play** (the bundle for Google Play: no self-updater, no permission to install apps).

## What stays and what changes for the family

Everything people saved and share lives in Firebase, tied to their Google account and their family,
not to the app ID. The Play app connects to the same Firebase project, so after signing in again
the family, chores, groceries, notes, messages and places are all there. Only per-phone choices
start over: alerts and quiet hours, language, "remind me" times, the calendars picked, and location
sharing (off until switched on). Kids who sign in without Google need a parent to approve the new
install again; when approving, pick their existing profile so their history stays.
Keep the current app installed until the new one works, then uninstall it (the new app installs
next to it, because the ID differs).

## Steps

### 1. Firebase (same project, new app)
1. Firebase console, the existing project, *Project settings > Your apps > Add app > Android*.
   Package name `com.punyplatypus.noscorre`. Do **not** create a new project.
2. Add the SHA-1 fingerprints (*Your apps > the new app > Add fingerprint*): the shared debug key's
   (the one used for the old app, see the main README), the upload key's (step 2) and, once the
   first bundle is uploaded, Google's app-signing key (*Play Console > Test and release > App
   integrity > App signing*). Without Google's, sign-in fails in the version installed from Play.
3. Download `google-services.json` again (it lists both apps) and replace the GitHub secret
   `GOOGLE_SERVICES_JSON`.

### 2. Upload key
```sh
keytool -genkeypair -v -keystore upload.jks -storetype JKS -alias upload \
  -keyalg RSA -keysize 2048 -validity 10000
keytool -list -v -keystore upload.jks -alias upload | grep SHA1     # for step 1.2
base64 -w0 upload.jks          # macOS: base64 -i upload.jks
```
GitHub secrets: `UPLOAD_KEYSTORE_BASE64` (that text), `UPLOAD_KEYSTORE_PASSWORD`,
`UPLOAD_KEY_ALIAS` (`upload`), `UPLOAD_KEY_PASSWORD`. Keep `upload.jks` and the passwords somewhere
safe; Google can reset a lost upload key, but it takes days.

### 3. Build and first upload
CI builds the signed bundle on every push (artifact `nos-corre-play-bundle`, a `.aab`). Download it
and upload it in *Play Console > Test and release > Testing > Internal testing > Create release*.
Accept Play App Signing when asked. Then do step 1.2's Google key.

### 4. Pages for the policy links
GitHub, *Settings > Pages*: Source "Deploy from a branch", this branch, folder `/docs`. The pages
are then at `https://ramonvidal.github.io/ChoreMe/privacy.html` and `.../delete-account.html`.

### 5. Play Console forms
Use `listing.md` (texts and graphics), `data-safety.md`, and `app-content.md` (app access,
content rating, target audience, background location declaration and demo video). The privacy
policy URL is the one from step 4.

### 6. Testing and release
1. **Internal testing** (up to 100 people, no waiting): the family installs from the opt-in link.
2. A **personal** developer account made after November 2023 must run a **closed test with at
   least 12 testers for 14 days in a row** before it can apply for production. Organization
   accounts are exempt.
3. **Production**: *Apply for production*, then Google reviews (usually a few days).

## Later versions
Raise nothing by hand: the version code is the CI run number. Push, download the new bundle, upload
it to the track you want. Play does the updating on people's phones.
