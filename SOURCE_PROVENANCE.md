# Source provenance

TG-MAX Ultimate uses NagramXF as its Android UI source and ports selected MAX client protocol operations from Komet. Exact revisions are recorded in `upstream.lock.json` and git submodules.

- NagramXF: https://github.com/Keeperorowner/NagramXF, revision `49ae647f6b5555f80beae945af1ae6f6d0bbf877`.
- Komet: https://github.com/KometTeam/Komet, revision `7bbb73bd2c05877e48b37f6ec6f096471a341404`.
- NagramXF retains Telegram and other contributors' copyright notices. Its root LICENSE and Komet's root LICENSE are GNU GPL version 3 texts. Existing per-file notices remain authoritative for the corresponding files.
- TG-MAX integration changes are in `integrations/nagram`, `max-backend` and `scripts/prepare_nagram_client.py`. Generated checkouts include TGMAX_CHANGES.md/JSON. No upstream authorship is claimed for these modifications.
- The binary frame format is ported from Komet `lib/core/webpush/max_web_protocol.dart`; transport behavior from `max_web_socket.dart`; request semantics from `lib/backend/modules/*`; device fingerprint from `lib/core/protocol/chat_cache_fingerprint.dart`.
- Komet Flutter UI is not embedded. No MAX-decompiled or WhiteMAX Smali classes/binaries are imported.
- MAX-messenger Bot API and React miniapp packages are not used to impersonate a personal-account API.

Nagram's own native dependencies and their licenses are retained by the prepared checkout. Native submodule revisions are fixed by Nagram's gitlinks. Source packages accompanying a release must contain the modified checkout and initialized dependencies, with notices retained. Root GPL text is included as LICENSE.

MAX protocol behavior may change independently of this repository. Operation names or opcode tables alone are not evidence of live-account support; release notes distinguish source tests, APK compilation and device/account validation.


The phone authentication adapter follows the official MAX web request flow inspected on 2026-11-13 at https://web.max.ru/. The official bundles `B8Wt1iAx.js` and `0.CO972B4e.js` use opcode 224 with source auth and identifier, then opcode 17 with captchaToken. The embedded VK CAPTCHA sends vk-sak-sdk messages including VKCaptchaGetResult. The TG-MAX bridge validates iframe source and origin and forwards the token from the user-completed check. It implements the protocol without bundling the official client code.
