# Releasing

Android releases go to Google Play through [fastlane](https://fastlane.tools). Every step runs in
GitHub Actions; the lanes in `fastlane/Fastfile` can also run locally.

## Release a version

1. **Release notes.** On a branch `chore/release-X.Y.Z`, add
   `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`, at most 500 characters. The
   versionCode of `X.Y.Z` is `X*10000 + Y*100 + Z`, so 0.2.0 is `200`. Translations are optional:
   the same file under another language's folder replaces the English notes there; write it for its
   readers, not word for word. Check them with
   `bundle exec fastlane android check_metadata version:X.Y.Z`, then open a PR and merge it.
2. **Tag.** `git tag vX.Y.Z && git push origin vX.Y.Z` on the merged commit. `release.yml` stops
   before building if the English release notes for the version are missing. It then builds the signed
   bundle and APK, uploads the bundle with its release notes to the **internal** track, and
   publishes the APK as a GitHub release.
3. **Promote.** After trying the internal build, run *Actions → Google Play → promote*. It moves
   the newest internal release to production for everyone. For a staged rollout set `rollout` to a
   share of users instead, e.g. `0.2` for 20%.
4. **Roll out.** A staged rollout is widened with *Google Play → rollout* (`0.5`, then `1` for
   everyone).

Tick *Dry run* to have Play validate a promote, rollout or listing change without applying it.

## Store listing

The title, short description and full description for every language are in
`fastlane/metadata/android/<locale>/`. Play's limits are 30, 80 and 4000 characters; the title and
short description are a single line without a final newline. The PR pipeline checks the limits
and that every app language (`androidApp/src/main/res/xml/locales_config.xml`) has a listing.

After merging a listing change, run *Actions → Google Play → listing*. It uploads the texts and
the phone and tablet screenshots of every language (screenshots only where they changed); the *screenshots*
task uploads only the screenshots. Listing changes can
go through Play's review before they show up.

## Screenshots

`fastlane/metadata/android/<locale>/images/` holds the screenshots of every language, stored in
Git LFS (`.gitattributes`): run `git lfs install` once, and `git lfs pull` if the files are small
text pointers. CI fetches them only for the uploads.

| Folder | Screen | Shown on Play | Uploaded by |
| --- | --- | --- | --- |
| `phoneScreenshots` | 1080×2160, 411dp | phones | *listing* / *screenshots* |
| `sevenInchScreenshots` | 1440×2560 portrait, 720dp | tablets, Chromebooks | *listing* / *screenshots* |
| `tenInchScreenshots` | 2560×1440 landscape, 1280dp, two panes | tablets, Chromebooks | *listing* / *screenshots* |
| `foldableScreenshots` | 4:3 and 1:1 unfolded, 10:16 folded | foldables | **by hand** |

The Play API has no foldable screenshots, so fastlane leaves that folder alone. In Play Console,
open *Store presence → Main store listing*, pick each language and drag that language's
`foldableScreenshots` into *Foldable screenshots*. `check_metadata` checks Play's rules for every
folder, including the 1:1, 4:3 and 10:16 foldable screenshots Play requires.

The screenshots come from the instrumented test `StoreScreenshots` in `androidApp/src/androidTest`.
It signs in to a fake server running inside the test and shows an invented collection
(`DemoLibrary`: made-up series, shops and drawn covers), dated relative to the day it runs. Where
the window is 840dp or wider, the app shows list and detail side by side and the test opens the
order and the series in the detail pane.

To take them again, run *Actions → Store screenshots*, optionally limited to some screen sizes. It
runs the test for every store language on an emulator and pushes the result to a branch
`store-screenshots/<run>`; the run summary links to open a pull request. After merging, run
*Google Play → listing* (or *screenshots*) to publish them, and upload the foldable ones by hand.

Locally, with an emulator or device on Android 13 or newer, run
`bundle exec fastlane android screenshots`. `devices:tablet` or `locales:en-US,de-DE` limit the
run. The lane switches the display to each screen size of `SCREENSHOT_SETS` in the Fastfile, turns
on SystemUI demo mode for a clean status bar, clears the app's data and sets everything back
afterwards. Nothing else may use port 8080 on the device, such as an `adb reverse` to a local
server.
## Running fastlane locally

Needs Ruby 4 (`.ruby-version`) and `bundle install` once. Without a local Ruby, Docker works too:

```sh
docker run --rm -v "$PWD":/w -w /w ruby:4.0 \
  sh -c 'bundle install --quiet && bundle exec fastlane android check_metadata'
```

Lanes that talk to Play read the service account key from `PLAY_SERVICE_ACCOUNT_JSON`.
`bundle exec fastlane android fetch_listing` downloads the live listing to `fastlane/metadata/play/`
(git-ignored) for comparison.
