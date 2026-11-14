<div align="center">

# MAXGRAM CLIENT

**The MAX messenger. The Telegram experience.**

Unofficial Android client built on [NagramXF](https://github.com/Keeperorowner/NagramXF), with a native MAX backend based on [Komet](https://github.com/KometTeam/Komet).

[Download APK](https://github.com/Lubara7/MAXGRAM-CLIENT/releases) · [Source code](https://github.com/Lubara7/MAXGRAM-CLIENT) · [Build guide](docs/nagram-migration.md)

</div>

## Android client

Telegram-style navigation, chat list and message bubbles, connected to MAX. Phone-number sign-in supports CAPTCHA, a verification code, an account password and server-selected outgoing-call verification. The login screen includes language selection, help and MAX legal documents.

**Languages:** Русский · English · Español · Português (Brasil) · O‘zbek · Français · Беларуская.

**Android 8.1+ · ARM64 · Experimental alpha.** Live-account compatibility is still being tested. Media uploads, stories, messenger voice/video calls and background MAX push are not implemented. Login translations are bundled; the wider Nagram interface still has English fallback strings. See [release notes](docs/release-notes.md).

## Build from source

Requires JDK 21, Android SDK 36, Build Tools 36.0.0, NDK 27.2.12479018 and CMake 3.31.6.

```bash
git clone https://github.com/Lubara7/MAXGRAM-CLIENT.git
cd MAXGRAM-CLIENT
git submodule update --init --depth 1 upstream/nagramxf upstream/komet
python3 scripts/prepare_nagram_client.py
python3 scripts/init_nagram_dependencies.py
cd build/nagram-client
NATIVE_TARGET=arm64-v8a ./gradlew :tgmax-backend:test :TMessagesProj:assembleNormalDebug
```

The prepared checkout uses real Nagram UI components and a Java MAX protocol adapter. MTProto transport is excluded from the native build. The Android application ID stays `io.github.lubara7.tgmax`, so updates preserve the installed application. CI reuses the alpha signing key.

## Development

- [Architecture and integration](docs/nagram-migration.md)
- [Authentication research and screen mapping](docs/auth-research.md)
- [Telegram / Nagram video screen inventory](docs/ui-video-reference.md)
- [Source provenance](SOURCE_PROVENANCE.md)
- [Release notes and limitations](docs/release-notes.md)

Backend tests cover binary frames, compression, CAPTCHA ordering, cancellation, OTP, call verification, sessions and stable message identifiers. CI also builds the APK, checks its signature and opens the published build in an Android emulator.

## License

Our integration code is distributed under [GNU GPL v3](LICENSE). Telegram, NagramXF and Komet notices remain in their source files. MAX artwork extracted from the user-supplied application retains its original ownership; provenance and hashes are recorded in [max-apk-assets.json](integrations/nagram/max-apk-assets.json).

MAXGRAM CLIENT is an independent project and is not affiliated with Telegram or MAX.
