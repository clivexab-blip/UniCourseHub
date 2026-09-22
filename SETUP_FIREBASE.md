# Setting up Firebase Authentication (required before the app will run)

This project uses **Firebase Authentication** for login/register (email+password and
"Continue with Google"). Everything else — courses, lessons, enrollments, and projects —
still lives entirely in the local **SQLite** database (`DatabaseHelper.kt`), keyed by a
`firebase_uid` column that links each local profile row to its Firebase account.

The repo ships with a **placeholder** `app/google-services.json` so the project opens and
the Gradle files are valid, but sign-in will not actually work until you swap in your own.

## 1. Create a Firebase project
1. Go to https://console.firebase.google.com and click **Add project**.
2. Give it any name (e.g. "UniCourse Hub").

## 2. Register the Android app
1. In the project, click **Add app → Android**.
2. Package name: `com.unicoursehub.app` (must match exactly).
3. Download the generated **google-services.json**.
4. Replace `app/google-services.json` in this project with the downloaded file.

## 3. Enable sign-in providers
In the Firebase console: **Authentication → Sign-in method** and enable:
- **Email/Password**
- **Google**

## 4. Add your SHA-1 (required for Google Sign-In)
Google Sign-In will fail with a `DEVELOPER_ERROR` unless your debug/release signing
certificate's SHA-1 is registered.

1. In Android Studio's terminal, run:
   ```
   ./gradlew signingReport
   ```
2. Copy the `SHA1` value under the `debug` variant.
3. In the Firebase console: **Project settings → Your apps → Add fingerprint**, paste it in.
4. Re-download `google-services.json` (it now contains your OAuth client) and replace the
   file in `app/` again.

## 5. Rebuild
Sync Gradle and run the app. Login, Register, and "Continue with Google" will now work
against your real Firebase project.

## How the two databases fit together
- **Firebase Authentication** — owns the email/password and Google sign-in, and issues a
  unique `uid` per account. It does **not** know a user's role, university, or student ID.
- **Local SQLite** (`users` table) — stores that extra profile info (`full_name`, `role`,
  `university`, `student_id`, `status`) keyed by `firebase_uid`.
- The first time a new Firebase account signs in, the app has no matching SQLite row yet,
  so it's routed to **CompleteProfileActivity** to pick a role and fill in the rest — after
  that, the profile is saved locally and reused on every future login.
- The demo seed accounts (Clive Xaba / Sphumelele Xaba / etc. in `DatabaseHelper.seedData`)
  exist only in SQLite with no Firebase account attached. If you register or sign in with
  Google using one of those exact email addresses, the app links your new Firebase account
  to that existing profile automatically instead of creating a duplicate.
