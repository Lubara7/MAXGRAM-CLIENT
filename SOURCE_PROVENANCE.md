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
