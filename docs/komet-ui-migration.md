# MaxGram on Komet

This experimental branch uses Komet as the MAX client and changes its Flutter presentation. It does not translate MAX traffic into Telegram MTProto. Existing Java Telegram screens cannot be mounted directly in Flutter; their appearance and interactions must be recreated using Komet's own state and callbacks.

Komet is pinned to `7bbb73bd2c05877e48b37f6ec6f096471a341404` (0.5.19+19). Its GPL-3.0 license, copyright and attribution remain in the prepared source. The published native MaxGram 0.3.0 development release remains a separate client.

## Implemented in this branch

| Area | Change | MAX behavior |
| --- | --- | --- |
| Global theme | Telegram-inspired light/dark palette, Roboto, circular compose button, compact toolbar | Existing theme, font and AMOLED preferences retained |
| Android chat list below 900 logical pixels | “Чаты”/“Chats” toolbar, drawer, search, larger avatars, underlined folder tabs | Existing chat, folder, story, search and selection callbacks |
| Navigation | Drawer routes to contacts, calls, saved messages, downloads and settings | Existing Komet screens, with back navigation for standalone routes |
| Login | Telegram-style phone screen, 44 requested countries, RU/EN selection, help and MAX legal links in the in-app browser, first-use disclaimer | Existing phone/code/password and token APIs; duplicate phone requests prevented; nonfunctional QR/session-file options removed |
| Code resend | Actual resend request, updated verification token, cooldown and concurrent-request protection | A failed resend preserves the current challenge; an old response cannot overwrite a new connection's challenge |
| History errors | Retryable errors remain errors instead of marking a conversation as exhausted; visible retry guidance | One reviewed patch in `lib/backend/modules/messages.dart`; loading state always reset |
| Branding and updates | MAXGRAM label, separate preview package, updater points to this repository | Only release tags ending in `-komet-ui` are eligible for this client |
| Backend | Protocol, transport, storage and remaining modules preserved | 53 of 54 protected files unchanged; the single reviewed history fix is locked by before/after SHA-256 |

Desktop, forwarding, sharing and archive routes retain Komet's navigation. Chat bubbles reuse Komet's existing TG Mobile/TG Desktop shape setting and adopt the new incoming/outgoing colors. This is the first presentation migration, not a completed 1:1 Telegram recreation.

## Prepare and check

```sh
git submodule update --init upstream/komet-ui
python3 scripts/prepare_komet_client.py
cd build/komet-client
CI=true FLUTTER_SUPPRESS_ANALYTICS=true flutter pub get
CI=true FLUTTER_SUPPRESS_ANALYTICS=true flutter analyze --no-pub --no-fatal-infos lib test
CI=true FLUTTER_SUPPRESS_ANALYTICS=true flutter test --no-pub
```

Use Flutter 3.44.3. The preparer checks the upstream commit, refuses to overwrite an existing output directory, applies the pinned patch and writes `maxgram-preparation.json`. A separate source checkout can be selected with `--source`; cloning still uses only its pinned commit, not its uncommitted changes. Analysis covers the complete Flutter application and tests (`lib` and `test`); the vendored Cargokit build tool is a separate Dart package with its own dependency resolution. The CI job performs analysis and widget tests, without producing an APK/AAB.

## Remaining work

1. Complete Telegram chat header, composer, media sheets, profile, privacy and settings layouts, preserving MAX-only capabilities. Replace inherited Komet artwork and remove remaining controls without a verified MAX equivalent.
2. Verify MAX phone/captcha/call confirmation and QR-device linking on a real test account. Retaining Komet's code does not establish that every MAX flow works or that bans cannot occur.
3. Connect and verify local feature settings such as ghost mode, deleted-message retention and typing suppression. Existing modules require behavioral tests before their reliability is claimed.
4. Supply MaxGram's own notification service configuration. The preview package is `io.github.lubara7.maxgram.preview`, flavor `maxgram`; Komet's bundled Firebase configurations belong to other application IDs and cannot configure this package. The Google services plugin is applied only when `android/app/src/maxgram/google-services.json` is supplied. Without it, Firebase initialization exits through Komet's existing error handler; remote background push is unavailable. No APK has been built for this branch.
5. Review upstream external services, notification registration, update asset selection, deep links and native platform branding before shipping the fork. Preserve license notices when replacing visible product names.
6. Define an explicit native-client data migration. Do not copy incompatible databases, silently import tokens, or promise upgrade compatibility with the older Java client.
7. Check remaining Telegram features individually. Local appearance, themes and selected animations can be reused; server-side Telegram features require MAX equivalents.

No real-account login, media, calls or 72-hour observation is claimed by this branch. Its preparation check verifies the pinned implementation and reviewed patch, not that the server accepts a session.
