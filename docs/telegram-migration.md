# Official Telegram / MAX integration

The Android UI is built from official DrKLO/Telegram 13.2.2 (7303), pinned to `21f7d109da6ab3506c9e9577f98c6a38771ab335`. `upstream/telegram` is the application source. `upstream/nagramxf` is a reference-only pin for selected privacy behavior; its application, menus, plugins and controllers are not compiled.

`prepare_telegram_client.py` creates a modified checkout, installs the MAX adapter, keeps the native Telegram UI and animation engine, removes MTProto socket/handshake/datacenter sources from the native graph, guards Java transport entry points and disables Telegram API credentials, updates, push registration and billing initialization. The release packager checks the compiled ARM64 graph. A complete network egress audit remains outstanding.

The official library module is `TMessagesProj`; the application is `TMessagesProj_AppStandalone:assembleAfatDebug`. The package remains `io.github.lubara7.tgmax`, without the upstream `.web` suffix. Version codes increase from alpha.8. CI restores the existing alpha signing key. MAX session encryption aliases and identifier-store paths are unchanged.

## Authentication

Phone/CAPTCHA/code, MAX password and server-selected outgoing-call verification reuse Telegram screens. The 44 supported countries retain the requested order; the seven login languages are Russian, English, Spanish, Brazilian Portuguese, Uzbek, French and Belarusian.

QR sign-in uses MAX commands 288 (create), 289 (status) and 291 (authorize by track). The QR contains the server-issued MAX link. Its lifetime and polling interval come from the server; timers use monotonic time, expired codes are cleared and refresh is explicit. Polling stops while paused. Authorization already confirmed on another device can finish while paused and is delivered when the screen resumes. Destroyed screens ignore late callbacks. Password challenges share the phone authentication completion path. Telegram login tokens are not used.

QR approval from the native Telegram device-scanner screen is still disabled; this release displays a MAX QR for scanning and approval in an existing MAX session.

## Local controls

| Control | Behavior |
|---|---|
| Unread mode | Suppresses MAX READ requests at the socket boundary, including requests queued before the setting changed |
| No typing | Suppresses MAX typing signals; the normal text indicator uses the MAX TEXT type |
| Ghost mode | Combines read/typing suppression and disables interactive activity flags on login and heartbeat packets |
| Animated stickers / emoji | Uses the native Telegram animation flags for assets available to the renderer |

Privacy settings are separate per account and persist locally. They apply to this device; other sessions and server rules can affect online visibility. They do not promise anonymity or grant a server subscription. Leaving ghost mode does not replay suppressed requests.

## Names, avatars and menus

Missing chat participants are fetched through MAX contact-info in bounded batches before dialogs are returned. Profile names and phone fields come from MAX. Unknown contacts use a localized label instead of a generated `MAX <id>` name. Contact avatar HTTPS URLs are loaded through the native image loader, rather than Telegram file locations.

The main settings list removes Money, Telegram Premium, Stars, Gram Earnings and Send a Gift. Purchase and wallet fragments are guarded and billing initialization is disabled. MAX for business opens https://business.max.ru/. Local features have their own native settings screen.

## Remaining work and validation

Tests use synthetic packets and a local WebSocket server. API-shape checks compile the adapter against extracted official Telegram members; only the full Android build verifies all screens. The published-APK check exercises seven login languages, ordered countries, legal layout, decoding fixtures and account-isolated local privacy settings.

Real-account QR confirmation, outgoing calls and live media still require device validation. MAX media uploads/downloads, sticker/emoji catalog and attachment mapping, stories, messenger calls, push, server privacy/settings and lossless gap recovery are not complete. Animation switches preserve the local renderer; they do not implement missing MAX media operations. Existing screens outside the adapted areas may still require MAX replacements. Password recovery/email and QR approval from MAXGRAM itself remain unimplemented.
