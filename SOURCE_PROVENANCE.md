# Source provenance

MAXGRAM CLIENT uses NextAlone/Nagram as its active Android UI source and ports selected MAX client protocol operations from Komet. NagramXF is consulted only for selected privacy behavior and is not compiled. Exact revisions are recorded in `upstream.lock.json` and git submodules.

- NextAlone/Nagram (active UI): https://github.com/NextAlone/Nagram, revision `8f2a8a03aa7aec599150b09d302bfd5023283344`, version 13.1.1 (7237), GPL-3.0-or-later.
- Telegram Android (compatibility reference): https://github.com/DrKLO/Telegram, revision `21f7d109da6ab3506c9e9577f98c6a38771ab335`, version 13.2.2 (7303). Upstream GPL-2.0-or-later file notices are retained.
- NagramXF (reference only): https://github.com/Keeperorowner/NagramXF, revision `49ae647f6b5555f80beae945af1ae6f6d0bbf877`.
- Komet: https://github.com/KometTeam/Komet, revision `7ce5814e625f61b553b92e13e09ee59d5a01057e` (`dev/0.5.0`).
- NagramXF retains Telegram and other contributors' copyright notices. Its root LICENSE and Komet's root LICENSE are GNU GPL version 3 texts. Existing per-file notices remain authoritative for the corresponding files.
- TG-MAX integration changes are in `integrations/telegram`, `max-backend` and `scripts/prepare_telegram_client.py`. Generated checkouts include TGMAX_CHANGES.md/JSON. No upstream authorship is claimed for these modifications.
- The binary frame format is ported from Komet `lib/core/webpush/max_web_protocol.dart`; transport behavior from `max_web_socket.dart`; request semantics from `lib/backend/modules/*`; device fingerprint from `lib/core/protocol/chat_cache_fingerprint.dart`.
- Komet Flutter UI is not embedded. No MAX-decompiled or WhiteMAX Smali classes/binaries are imported.
- MAX-messenger Bot API and React miniapp packages are not used to impersonate a personal-account API.

Telegram's native dependencies and their licenses are retained by the prepared checkout. Native submodule revisions are fixed by Telegram's gitlinks. Source packages accompanying a release must contain the modified checkout and initialized dependencies, with notices retained. Root GPL text is included as LICENSE.

MAX protocol behavior may change independently of this repository. Operation names or opcode tables alone are not evidence of live-account support; release notes distinguish source tests, APK compilation and device/account validation.


The phone authentication adapter follows the official MAX web request flow inspected on 2026-11-13 at https://web.max.ru/. The official bundles `B8Wt1iAx.js` and `0.CO972B4e.js` use opcode 224 with source auth and identifier, then opcode 17 with captchaToken. The embedded VK CAPTCHA sends vk-sak-sdk messages including VKCaptchaGetResult. The TG-MAX bridge validates iframe source and origin and forwards the token from the user-completed check. It implements the protocol without bundling the official client code.


Authentication and codec update (2026-11-13):

- Komet dev/0.5.0 `account.dart`, `account/account_models.dart`, `call_out_controller.dart` and `opcode_map.dart` establish outgoing-call params, statuses, opcode 294 checks and opcode 291 verification. The Java integration is adapted to Nagram's existing authorization/registration/password transitions.
- Trollobot/Telemax revision `58dfb72745911540a137b0a568561790ce9bc0cc`, `src/max/frame.ts`, `src/max/msgpack.ts` and tests document extension 1 nesting, consecutive MessagePack values and compression flag 0xFF (Zstandard). These files were studied for interoperability; their TypeScript source is not bundled.
- BEARlogin/max-telegram-bridge-bot revision `af5c7f81f0f467040aadf658e2183ead63951af2` was reviewed for message mapping, media limits and retry queues. It uses bot APIs and is not a personal-account authentication implementation. Its source is not embedded.
- WhiteMAX-Mod/whitemaxmod revision `1d0749f0012c745b8cd68aca8a97c4cd814324cc`, version 26.34.1, was consulted for call verification labels and legal links. Its modified Smali/runtime is not embedded.
- DrKLO/Telegram revision `21f7d109da6ab3506c9e9577f98c6a38771ab335`, `LoginActivity.java` and README were compared with the pinned Nagram-derived screens. No second Telegram transport is introduced.
- The user-supplied MAX.apk is version 26.19.2 (6733). Apktool 3.0.3 decoded the manifest/resources; JADX 1.5.6 was used for targeted inspection of InputPhoneScreen, including its legal-link spans. Recovered code is reference material, not compiled into this client. Icon and two JSON animation resources are retained with their original ownership; hashes are in `integrations/telegram/max-apk-assets.json`. The original APK is not committed.
- Zstandard decoding uses `com.github.luben:zstd-jni:1.5.7-7`: the official Android AAR is packaged in the APK; the desktop JAR is used only for JVM builds/tests. JNI bindings use BSD-2-Clause and bundled Zstandard is used under BSD-3-Clause. Their complete copyright and license notices are retained in `integrations/telegram/src/main/assets/tgmax-zstandard-notices.txt` and packaged in the APK. Source: https://github.com/luben/zstd-jni. The previous Airlift decoder was removed after an installed-APK instrumentation test exposed its dependency on a `sun.misc.Unsafe` field absent from Android. No native binary is downloaded at runtime.

Official Telegram migration (2026-11-14):

- Privacy semantics were compared with pinned NagramXF `com/radolyn/ayugram/AyuGhostConfig.java`. MAXGRAM implements its own small MAX transport policy; the Nagram/AyuGram application, controllers, menus and plugins are not embedded.
- Official MAX web bundles `0.r1iFaaS8.js` and `RmCTWSHK.js` at https://web.max.ru/ were inspected for QR creation/status/authentication (288/289/291), server TTL, `status.loginAvailable`, TEXT typing (65) and interactive presence pings (1). Their JavaScript is reference material and is not bundled.
- Komet `lib/backend/modules/contacts.dart` confirms contact names, photoId, baseUrl/baseRawUrl and direct HTTPS avatar loading. Personal screenshots and account data are not included.
- Komet `lib/backend/modules/chat_parsing.dart` establishes group/channel `baseIconUrl`; the native image loader is routed to that HTTPS URL.

Nagram migration (2026-11-15): Nagram cache schema 179 is extended to 181 for compatibility with previously installed MaxGram builds. Existing message and user TL constructor IDs were compared. Widget cache data is preserved in a backup table during conversion; message tables are not dropped.

Account/settings review (2026-11-15): the user-provided MAX 26.19.2 APK (SHA-256 `a658458209fb9cb3e57bf544eeb3b61a7b16685d2b595ccc9a5665f1e4f4c92f`) was inspected with JADX 1.5.6. Targeted readable output was checked despite unrelated decompiler errors. `dvk.d` defines `1M`/`3M`/`6M`; `dvk.a()` serializes `INACTIVE_TTL`; CONFIG opcode 22 writes `settings.user` and returns `user`. Opcode 200 reads a deletion timestamp rather than setting inactivity. `jk4`/`hk4` confirm CONTACT_UPDATE 34 with contactId and BLOCK/UNBLOCK; `tg4` confirms CONTACT_LIST 36 with status BLOCKED, from and count. Recovered application classes are not compiled into MaxGram. New Java adapters implement the independently recorded wire structures. PyMax self service/payloads and Komet contacts.dart confirm profile and privacy operations.
