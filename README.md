<div align="center">

# MAXGRAM CLIENT

Specification v2 is in progress: see [implementation and acceptance status](docs/spec-v2-status.md). APKs remain prototypes. The [dev.3 test APK](https://github.com/Lubara7/MAXGRAM-CLIENT/releases/tag/v0.3.0-dev.3) is published as an explicitly requested experimental Pre-release. Verified releases require matching live-account acceptance evidence and a completed 72-hour observation. Neither a session profile nor a successful observation guarantees immunity from later account restrictions.

**The MAX messenger. The Telegram experience.**

Unofficial Android client based on [Komet](https://github.com/KometTeam/Komet), with a Telegram-style Flutter interface.

[Test APK release](https://github.com/Lubara7/MAXGRAM-CLIENT/releases/tag/v0.3.0-dev.3) · [Source code](https://github.com/Lubara7/MAXGRAM-CLIENT) · [Build guide](docs/telegram-migration.md)

</div>

The current direction uses pinned Komet sources under GPL v3. The older Nagram/Java client remains in this repository for reference.

## Komet preview

The `komet-telegram-ui` branch contains the newer Komet-based client direction: [preparation, implemented changes and limitations](docs/komet-ui-migration.md). It preserves Komet's MAX implementation and recreates Telegram-style screens in Flutter. The published 0.3.0 APK belongs to the older Java client and does not contain these Flutter changes.

Implemented changes include the Telegram-style theme and chat drawer, MAX phone login with the requested 44 countries, RU/EN, help/legal links, duplicate-submit protection, actual OTP resend and retryable message-history errors. Komet's contacts, calls, media and local-feature implementations remain available for further verification.

Use Flutter 3.44.3. Initialize `upstream/komet-ui`, run `python3 scripts/prepare_komet_client.py`, then run `flutter pub get`, `flutter analyze --no-pub --no-fatal-infos lib test` and `flutter test --no-pub` inside `build/komet-client`. Set `CI=true FLUTTER_SUPPRESS_ANALYTICS=true` for these commands. The preview package is `io.github.lubara7.maxgram.preview`; no APK has been produced from this branch. Full Telegram UI parity and real-account verification remain unfinished.

## Legacy native client

Telegram-style navigation, chat list and message bubbles, connected to MAX. The backend can request and poll a server-issued QR challenge; authenticated QR approval in Devices remains unfinished and QR is absent from the login screen. Phone-number sign-in supports CAPTCHA, a verification code, an account password and server-selected outgoing-call verification. The login screen includes language selection, help and MAX legal documents.

**Languages (specification v2):** Русский · English.

**Android 8.1+ · ARM64 · Experimental alpha.** Live-account compatibility is still being tested. Media uploads, stories, messenger voice/video calls and background MAX push are not implemented. Login translations are bundled; the wider Telegram interface still has English fallback strings. See [release notes](docs/release-notes.md).

## Build the legacy native client

Requires JDK 21, Android SDK 36, Build Tools 36.0.0, NDK 27.2.12479018 and CMake 3.31.6.

```bash
git clone https://github.com/Lubara7/MAXGRAM-CLIENT.git
cd MAXGRAM-CLIENT
git submodule update --init --depth 1 upstream/nagram
python3 scripts/prepare_telegram_client.py
python3 scripts/init_telegram_dependencies.py
cd build/telegram-client
NATIVE_TARGET=arm64-v8a ./gradlew :tgmax-backend:test :TMessagesProj:assembleDebug
```

The prepared checkout uses real Telegram UI components and a Java MAX protocol adapter. MTProto transport is excluded from the native build. The Android application ID stays `io.github.lubara7.tgmax`, so updates preserve the installed application. CI reuses the alpha signing key.

## Development

- [Architecture and integration](docs/telegram-migration.md)
- [Authentication research and screen mapping](docs/auth-research.md)
- [Telegram / Nagram video screen inventory](docs/ui-video-reference.md)
- [Source provenance](SOURCE_PROVENANCE.md)
- [Release notes and limitations](docs/release-notes.md)

Backend tests cover binary frames, compression, CAPTCHA ordering, cancellation, OTP, call verification, sessions and stable message identifiers. CI also builds the APK, checks its signature and opens the matching prototype artifact in an Android emulator. [The dev.3 installed-device evidence](docs/validation/android-smoke-0.3.0-dev.3.json) records the exact APK hash and successful automated checks; live-account acceptance remains pending.

## License

Our integration code is distributed under [GNU GPL v3](LICENSE). Telegram, NagramXF and Komet notices remain in their source files. MAX artwork extracted from the user-supplied application retains its original ownership; provenance and hashes are recorded in [max-apk-assets.json](integrations/telegram/max-apk-assets.json).

MAXGRAM CLIENT is an independent project and is not affiliated with Telegram or MAX.
