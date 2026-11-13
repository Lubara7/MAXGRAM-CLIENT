# MAXGRAM CLIENT: authentication and source review

The Android UI remains the pinned NagramXF source, derived from Telegram. The transport uses MAX binary WebSocket frames. Bot bridge projects are useful references for message mapping but cannot authenticate a user's MAX phone account.

| Source | Inspected implementation | Applied decision |
|---|---|---|
| DrKLO/Telegram | LoginActivity phone form, code transitions, login views; source/licensing README | Retain Telegram layout, outlines, typography, keyboard and authorization transitions |
| Komet dev/0.5.0 | Account module, call-out models/controller, opcode table, binary codec | Server-selected OTP or outgoing-call flow; 294 status polling, 291 verification, shared password/registration completion |
| Trollobot/Telemax | Binary header, compression, MessagePack extensions/streams and tests | Accept nested extension 1, multiple top-level MessagePack values and bounded Zstandard decoding |
| BEARlogin/max-telegram-bridge-bot | MAX listener, attachment parser, formatting and retry/queue design | Keep durable identifier mapping; distinguish bot API limits from personal-client protocol capabilities |
| WhiteMAX | 26.34.1 resource strings and call params Smali | Cross-check call labels and legal recommendation URL; no modified binary/Smali inclusion |
| User-supplied MAX.apk | 26.19.2 manifest/resources and targeted InputPhoneScreen decompilation | Extract launcher artwork, two animation files, legal links and verify the original clickable-span behavior |

Exact source revisions and asset hashes are in SOURCE_PROVENANCE.md and the asset manifest. The supplied APK predates the newer outgoing-call screen, so it is not evidence for the new call protocol by itself.

## Screens

| MAX screen/control | MAXGRAM presentation and behavior |
|---|---|
| Phone input | Existing Telegram PhoneView with the requested MAX instruction and 44-country order |
| Language selector | Telegram LanguageCell bottom sheet; seven rows in screenshot order, native name and localized secondary name |
| Help | Question mark in a circle at the existing Telegram top-right menu position, opening https://max.ru/support |
| Legal footer | Privacy https://legal.max.ru/pp, agreement https://legal.max.ru/ps, recommendations https://legal.max.ru/recsysrules |
| CAPTCHA | User-completed official VK challenge in a Telegram bottom sheet; verified origin/source bridge; cancellation sends no OTP request |
| OTP | Telegram code view after MAX returns a valid token and codeLength |
| Outgoing call | Telegram colors/type/back button; server source track, destination phone, TTL and polling interval; system ACTION_DIAL |
| Account password | Existing Telegram-style password view connected to MAX passwordChallenge |
| Registration | Existing name form connected to MAX registration token |

There is no automatic CAPTCHA solving, automatic call placement or switch to Telegram servers.

## Protocol correction

Previously a valid extended/streamed MessagePack response or Zstandard-compressed frame could terminate the socket with MAX_PROTOCOL_ERROR, even after the server had sent an SMS. The decoder now supports those representations with frame-length, nesting, UTF-8, collection and decompression bounds retained. A small LZ4 output hint can expand only up to 32 MiB. Malformed responses still fail and provide a safe reason code, never their token/phone/payload content.

Tests use synthetic auth responses, not a captured user's account trace. They establish decoder interoperability, not proof that the reported live failure has been reproduced and eliminated on every server response.

## Call lifetime

The call screen starts a monotonic deadline when displayed, polls immediately and then at the server interval, and never overlaps checks. Status 0 waits, 1 enables the validated destination number, 2 completes auth by track and 3 expires. Expiry disables the old dial action; refresh is explicit. Leaving the screen cancels scheduled callbacks, and late completions cannot navigate. The server alone selects this method; there is no invented call token or fixed two-minute TTL.

## Remaining work

Validate the complete phone/CAPTCHA/code and outgoing-call flows on a real account. Media, push, stories, messenger calls, full settings adaptation and complete translations outside the login flow remain subsequent migration work. The release notes distinguish these from the login changes.
