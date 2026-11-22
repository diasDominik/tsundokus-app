fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android check_metadata

```sh
[bundle exec] fastlane android check_metadata
```

Checks the store listing and release notes against Play's limits and the app's languages

version:1.2.3 also requires English (en-US) release notes for that version; other languages

are optional. Screenshots still in Git LFS are only counted, unless images:true requires them

### android internal

```sh
[bundle exec] fastlane android internal
```

Uploads the release App Bundle to the internal track with its release notes

aab:<path> mapping:<path> default to the Gradle release outputs

### android promote

```sh
[bundle exec] fastlane android promote
```

Promotes an internal release to production, for everyone by default

rollout:0.2 starts a staged rollout to 20% instead; version:1.2.3 picks the release, the newest

internal one otherwise

### android rollout

```sh
[bundle exec] fastlane android rollout
```

Changes the share of users the production rollout reaches, e.g. rollout:0.5; rollout:1 completes it

### android listing

```sh
[bundle exec] fastlane android listing
```

Uploads the store listing in every language: title, descriptions and phone screenshots

screenshots_only:true leaves the texts on Play as they are

### android screenshots

```sh
[bundle exec] fastlane android screenshots
```

Takes the store screenshots in every store language on a running emulator or device

(Android 13+). The app runs against a fake server inside the test (StoreScreenshots), so

nothing may listen on the device's port 8080. skip_build:true reuses the APKs already built;

serial:<adb serial> picks the device when several are connected

### android fetch_listing

```sh
[bundle exec] fastlane android fetch_listing
```

Downloads the listing as it is on Play to fastlane/metadata/play (git-ignored), to compare

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
