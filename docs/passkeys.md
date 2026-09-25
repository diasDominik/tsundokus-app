# Passkeys

A passkey is a second way into an existing Tsundoku account. The password flow is unchanged: a
signed-in user enrols a passkey from **Settings → Passkeys**, and the login screen then offers
**Sign in with a passkey**. Nothing here creates accounts or recovers them.

## Which password manager can hold the passkey

Any of them. The server never asks for a particular kind of authenticator, which is what keeps
third-party managers such as 1Password eligible:

| Option the server sends | Value | Why it matters |
| --- | --- | --- |
| `authenticatorAttachment` | *unset* | Setting it to `platform` would limit the ceremony to the device's own authenticator and hide 1Password, Bitwarden and security keys. |
| `residentKey` | `required` | The credential is discoverable, so signing in needs no email — the manager offers the right passkey on its own. |
| `userVerification` | `required`, and checked again on the way back | The passkey is the whole sign-in here, not a second factor, so the request asks the authenticator to verify the user, and the server checks the User Verified flag itself on the response (WebAuthn L2 §7.2 step 17). Asking matters: a manager that is already unlocked otherwise answers without verifying anyone — 1Password returns `UV=0` in that case and the sign-in is refused. |

Where each provider can serve a passkey:

| Provider | Android | iOS | Desktop / Web |
| --- | --- | --- | --- |
| 1Password | Android 14+, with 1Password enabled under *Settings → Passwords, passkeys and autofill* | iOS 17+, with 1Password enabled under *Settings → General → AutoFill & Passwords* | Browser extension |
| Google Password Manager | Android 9+ | — | Chrome |
| iCloud Keychain | — | iOS 16+ | Safari |

Android 13 and below only offer the platform provider, so 1Password cannot serve a passkey there
even though the app itself runs from Android 8 — below Android 9 the passkey UI is hidden
altogether (`isPasskeySupported()`).

## What the domain has to publish

A provider only offers a passkey for `tsundokus.uk` if the domain vouches for the app:

- `/.well-known/assetlinks.json` — needs the `delegate_permission/common.get_login_creds` relation
  and the SHA-256 of **every** certificate the app ships under. Both the Play app signing
  certificate and the debug certificate are listed.
- `/.well-known/apple-app-site-association` — needs `webcredentials` naming `TEAMID.uk.tsundokus`.
  Rendered at deploy time from the `APPLE_TEAM_ID` secret, and not published while that is unset:
  Apple caches this file, so a wrong one is worse than none.

Both live in the server repository and are served by Caddy from `/srv/static`.

## Testing with 1Password

1. Deploy the server, so the two `.well-known` files are live on `tsundokus.uk`.
2. Install 1Password on the device and enable it as a passkey provider in system settings.
3. Sign in with a password, then **Settings → Passkeys → Add a passkey**. 1Password should appear in
   the provider sheet alongside the platform option; save the passkey to a vault.
4. Sign out, then **Sign in with a passkey**. 1Password should offer the saved passkey without an
   email being typed — that is the discoverable credential doing its job.
5. The account's list under **Settings → Passkeys** should show the new passkey, and removing it
   there should stop it working (the credential stays in 1Password's vault; the server no longer
   trusts it).

1Password asks to unlock the vault during sign-in even when it is already unlocked. That prompt *is*
the user verification the server requires; without it there is nothing to tell the account's owner
from whoever is holding the phone.

## When it does not work

- **The provider sheet skips 1Password**, or it refuses to save with *"the URL for this passkey does
  not match the selected application"*: the domain does not vouch for this build. `assetlinks.json`
  must carry the SHA-256 of the certificate the installed app is actually signed with — for a Play
  install that is Google's app signing key, not the upload key. Read it off the device rather than
  the console:

  ```
  adb shell pm path uk.tsundokus
  adb pull <base.apk> && apksigner verify --print-certs base.apk
  ```

  Google caches the file for about an hour (`maxAge` on `digitalassetlinks.googleapis.com`).

- **Sign-in is refused with a 401** after the provider happily signed: the server said no. It logs
  the reason — the authenticator flags, the counter, the origin and whether a user handle came
  back — on one line beginning `Passkey sign-in refused:`.
