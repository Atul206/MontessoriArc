# Release signing key

This directory is gitignored on purpose — the `.jks` file and
`keystore.properties` created here contain the secret that signs every
release build. They must never be committed.

## One-time setup

Run this yourself (not via an agent) so you're the only one who ever sees
the passwords. Pick your own passwords when prompted — do not reuse
another account's password.

```bash
cd keystore
keytool -genkeypair -v \
  -keystore calm-coloring-release.jks \
  -alias calmcoloring \
  -keyalg RSA -keysize 2048 -validity 10950
```

It will prompt for a keystore password, your name/organization details
(anything reasonable — this is just embedded in the certificate, Play
Store doesn't care), and a key password (can be the same as the keystore
password).

Then create `keystore/keystore.properties` (same directory) with:

```properties
storeFile=calm-coloring-release.jks
storePassword=<the keystore password you just set>
keyAlias=calmcoloring
keyPassword=<the key password you just set>
```

Once that file exists, `composeApp/build.gradle.kts` picks it up
automatically and `./gradlew :composeApp:bundleRelease` produces a signed
`.aab` at `composeApp/build/outputs/bundle/release/composeApp-release.aab`.

## Back this up

- **Back up `calm-coloring-release.jks` and the two passwords somewhere
  durable** — a password manager, encrypted cloud storage, wherever you
  keep secrets you can't afford to lose. If this file only exists on this
  one machine and the disk dies, you cannot publish updates to this app
  under the same listing again.
- Play Store uses **Play App Signing**: the key you generate here is only
  your *upload* key (used to prove each build comes from you); Google
  holds the actual signing key that ends up on users' devices. If you
  ever lose this upload key, Play Console has an upload-key-reset flow
  that Google support can walk you through — annoying, but not fatal.
  Losing it before you've ever uploaded a build is harmless; just
  generate a new one.
