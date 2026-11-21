# Contributing

How SeniorNav is built and what a change has to respect. How the code fits
together is in [ARCHITECTURE.md](ARCHITECTURE.md).

## Ground rules

- **Simplicity is the product.** Two buttons, Back and Home, always in the same
  place. No Recents, no gestures, no customisation, no moving the bar. A pull
  request that adds a feature will most likely be declined; one that removes
  complexity is welcome.
- **No dependencies.** Plain Java against the Android platform APIs. No
  AndroidX, Kotlin, Compose or third-party libraries. `testImplementation` of
  JUnit is the only exception.
- **Read nothing.** The accessibility service only looks at which windows are
  open. It never reads window content, text or keys, and the app has no internet
  access. This is what the Play accessibility policy and the users' trust rest on.
- **English and German.** Every user-visible string goes in both
  `res/values/strings.xml` and `res/values-de/strings.xml`.
- **Big and plain.** High contrast, state shown in words as well as by colour.

## Building

Needs JDK 17 and the Android SDK (API 35). Point Gradle at the SDK with a
gitignored `local.properties` containing `sdk.dir=/path/to/Android/Sdk`.

    ./gradlew assembleDebug     # app/build/outputs/apk/debug/SeniorNav-<version>-debug.apk
    ./gradlew assembleRelease   # signed only if signing is configured, see below

## Testing

Unit tests carry the weight; device testing is by hand for now.

    ./gradlew test        # JVM unit tests (BarRules, BarHeight, NavMode, BarStatus, Palette)
    ./gradlew lint

Logic that can be tested without a device belongs in a pure Java class (no
`android.*` imports) with a JVM test, like `BarRules`.

On a device: install, open the app, turn the service on, then check in a few
apps (including WhatsApp and a full-screen video) that Back and Home work, that
the bar steps aside for the keyboard and the lock screen, and that it follows a
rotation.

## Release signing

Run once:

    scripts/setup-signing.sh

It creates `~/keys/seniornav-release.jks`, stores a random password in the
system keyring (`secret-tool lookup service seniornav key release-keystore`)
and writes the gitignored `keystore.properties`. Back up the keystore and the
password off the machine: without them no future version can update installed
copies. Without any of this the release build is unsigned; debug builds and
tests are unaffected.

## Releasing

1. Bump `appVersionName` and `appVersionCode` in `app/build.gradle.kts`.
   The version code must always go up, or phones refuse the update.
2. `./gradlew test lint assembleRelease`
3. Check that the output is `app/build/outputs/apk/release/SeniorNav-<version>-release.apk`.
   An `-unsigned` name means signing was not configured.
4. Verify the signature with `apksigner verify --print-certs <apk>` against the
   release certificate below. A different certificate means the wrong key: do
   not publish.
5. Tag `v<version>` and attach the APK to a GitHub release, renamed to
   `SeniorNav-<version>.apk`. The release notes are for the people using the
   app: what changed for them, in plain words, in English and German. Technical
   detail (signing, checksums, internals) stays in this file and in
   ARCHITECTURE.md; the same goes for the README.

### Release certificate

Every release is signed with this key (`CN=SeniorNav, C=DE`); `apksigner` must
print exactly:

    Signer #1 certificate SHA-256 digest: 255e8429f716fd5c8c4d1ffe6731e4d4b13b1a211799fc232e6908f188b2ceb1

To check a downloaded APK: `apksigner verify --print-certs SeniorNav-<version>.apk`.
