# MaxGram: specification v2 and acceptance

The user's v2 specification supersedes the previous UI-base and login-language decisions:

- Requested Android base: NextAlone/Nagram, main commit `8f2a8a03aa7aec599150b09d302bfd5023283344` inspected on 2026-11-14. The active build now uses this Nagram pin. Historical published APKs still use the previous DrKLO base.
- Login languages: Russian and English. The prepared Nagram interface is restricted to these two languages.
- QR approval belongs in authenticated Settings → Devices. A new device displays a challenge; an authenticated device scans and approves it. The login-screen QR shortcut has been removed; authenticated approval is still pending.
- Remove unsupported controls instead of sending unsupported Telegram RPCs. Local ghost/unread/no-typing remain MAX transport policies.
- Android contacts, bot identity, presence, all media directions, formatting, internal links, MAX privacy/blacklist, profile navigation, automatic emoji animation, debug isolation, sounds and themes remain explicit acceptance items.
- Account settings: first name, last name, biography and server-backed account deletion/inactivity options only. The official supplied APK confirms inactivity is CONFIG 22 with `settings.user.INACTIVE_TTL` (`1M`, `3M`, `6M`). Opcode 200 only reads a scheduled deletion timestamp. Account settings use the server inactivity field and read it back; immediate deletion is not imitated.
- Opt-in deleted-message retention preserves messages received by this installation. It cannot recover messages deleted before receipt or while retention was disabled. Local delete, clear history and logout must have explicit purge behavior.

## Source review

Primary sources inspected (full commits are recorded in `integrations/max-session-profiles.json`):

- [PyMax session payloads](https://github.com/MaxApiTeam/PyMax/blob/c4cb917d354a1179be625925530d72c8aaa969ff/src/pymax/api/session/payloads.py): WEB handshake has `userAgent`/`deviceId`; mobile handshake also has `mt_instanceid` and `clientSessionId`. Mobile-only fields must not be blindly added to WEB payloads.
- [PyMax configuration](https://github.com/MaxApiTeam/PyMax/blob/c4cb917d354a1179be625925530d72c8aaa969ff/src/pymax/config.py): its ID generator uses `secrets.token_hex(8)` (16 hexadecimal characters). The example in the supplied specification contains 32 hexadecimal characters. The current official WEB implementation instead uses `crypto.randomUUID()`; therefore no universal hex-only rule is valid. Existing device IDs bound to tokens must be preserved, not reformatted automatically.
- [PyMax session restoration](https://github.com/MaxApiTeam/PyMax/blob/c4cb917d354a1179be625925530d72c8aaa969ff/src/pymax/app.py): saved device identity and instance ID are restored; regenerating them at every process launch is not a universal requirement.
- [PyMax version catalog](https://github.com/MaxApiTeam/PyMax/blob/c4cb917d354a1179be625925530d72c8aaa969ff/src/pymax/versions/catalog.py): mobile app version is coupled to a build fingerprint. Historical `25.12.14`/`0x97CB` are not a verified current default.
- [Komet spoofing service](https://github.com/KometTeam/Komet/blob/7bbb73bd2c05877e48b37f6ec6f096471a341404/lib/core/storage/spoofing_service.dart): per-account/pending profiles, version reset and device identity persistence. Komet and PyMax use different version recommendations; neither source proves ban immunity.
- `icyfalc0n/maxcalls`, `junioraww/maxplus`, `eizemazal/avenarius` and `pr0bel1230/max-api-docs` have been retrieved for API/call/media comparison. Official `max-messenger` repositories expose Bot API clients and are not a replacement for the user-account protocol.

The live official WEB assets reviewed on 2026-11-14 are `BWj-8FBy.js` (WEB version `26.11.6`, endpoint `wss://api.oneme.ru/websocket`) and `RmCTWSHK.js` (protocol version 11, WEB UUID generator, `WEBPUSH` push type, device locale separate from interface locale). Their hashes are recorded with the session catalog. The web local-storage `__oneme_auth` value is JSON containing `viewerId` and `token`; importing the whole JSON as a token is incorrect.

Current PyMax telemetry sends `Opcode.LOG` (5), not the guessed 1103 in the supplied text; it is disabled by default. Synthetic always-on `CHAT_LIST` telemetry is not a verified requirement and is not enabled.

Citation placeholders such as `[citation:13]` in the supplied text are not independently traceable sources. The claim about approximately 700 bans and any universal detection claim remain unverified.

## Publication gate

`Build MaxGram APK` creates prototype artifacts and cannot publish GitHub Releases. `Install prototype APK` tests the exact build artifact instead of downloading an older release.

`Publish verified MaxGram APK` is a separate workflow. It downloads the already tested APK and matching source without rebuilding them. `scripts/validate_release.py` requires the APK hash, source revision, session-profile hash, a current official-version review, every acceptance check and a completed observation window of at least 72 hours. Missing, future-dated, mismatched or ban-positive evidence blocks publication.

`docs/validation/acceptance.json` is intentionally pending. No live-account observation has been performed. A 72-hour successful observation is a release criterion, not a guarantee against a later restriction. Do not put tokens, phone numbers, verification codes or unredacted protocol frames in the evidence or public artifacts.

## Current known defect

The alpha.11 Android test detected insufficient Russian legal-footer spacing after a language switch. That build is a prototype; its successful compilation is not a completed UI acceptance test. The next artifact must resolve and retest this defect. The Nagram dev.1 artifact compiled successfully, but its installed test failed before login: the upstream publisher certificate pin aborted JNI_OnLoad and left NativeByteBuffer methods unregistered. The fork preparation now removes this upstream publisher pin; Android APK signing and the saved MaxGram signing-key checks remain. A synchronous native-buffer round trip runs before storage queues, and Android instrumentation verifies it again. These fixes still need an installed-artifact pass.

## Session-core implementation checkpoint

Wire-specific profile models now separate WEB metadata from mobile `buildNumber`/instance/session fields. New WEB identities follow the official UUID generator; the mobile helper uses 16 hexadecimal characters. Previously stored IDs are never normalized. Existing prototype sessions retain their legacy version and metadata instead of being silently converted to a new profile.

The Android adapter saves and restores its browser User-Agent. If none is stored, WebView initialization is posted to the main loop and the connection waits asynchronously. It no longer starts a restored session with a fabricated `MAXGRAM-CLIENT/...` browser header. One snapshot supplies both the HTTP User-Agent and INIT payload; closing while waiting for initialization fails pending requests. JVM tests cover delayed WebSocket upgrade/profile changes, closure before initialization, WEB/mobile field separation and identity preservation.

Profile settings now expose R.S.V, reviewed version selection, next-login default updates and explicit apply/local sign-out. Own WEB-session import preserves the original INIT together with the token in an encrypted v2 record; previous v1 records remain readable. Mobile TCP transport is still pending. First-launch consent is enforced before MAX connections, with RU/EN acknowledgement and refusal paths. The mobile payload model does not enable an Android/TCP login and must not be advertised as such. The legal-footer correction now reduces excess form gaps while keeping minimum separation; it still needs the next installed-APK test.


## History and deleted-copy checkpoint (2026-11-15)

MAX history uses timestamps; Telegram uses an anchor message and signed additional offsets. The adapter now keeps the anchor millisecond, sorts by time and server ID, supports older/newer/around offsets and filters min/max local IDs. Dense timestamp boundaries expand the MAX window instead of silently returning an empty page. Chat metadata is hydrated from the history response.

Deletion notifications differ: WEB sends a `REMOVED` message with opcode 128, while mobile also uses opcode 142 with a chat object and message IDs. The normalizer follows pinned PyMax `types/events/message.py` and `dispatch/resolvers.py`. Edits are not treated as new messages.

Deleted-copy retention is opt-in, per account and limited to snapshots received after activation. Message bodies use Android Keystore AES-256-GCM with chat/message-bound authentication data in a no-backup SQLite store. Retained messages carry a visible local-copy label. The adapter purges explicit own deletions, disabling retention and logout; archive failures must not prevent ordinary incoming messages from rendering. Android instrumentation covers encryption, account isolation, disabled defaults, unknown messages and purge behavior. Live MAX integration remains pending.


Account controls use the native Nagram first-name, last-name and biography fields plus a server inactivity selector. Privacy uses only the six MAX controls confirmed in PyMax and the supplied APK, with CONFIG read-back after updates. Blocklist retrieval paginates MAX contacts with status BLOCKED; block/unblock uses contactId/action rather than Telegram endpoints. Profile loading now maps MAX contact descriptions into native user-full responses. These are source/fixture implementations pending live-account checks, not acceptance claims.


WEB wire-format correction: pinned PyMax `protocol/ws/protocol.py` uses text JSON v11, whereas the previous Komet-derived codec sends binary v10. New WEB and imported browser profiles now use JSON v11. Legacy saved profiles retain binary v10 until explicit relogin. The JSON decoder preserves 64-bit IDs exactly, bounds nesting, checks reply/opcode correlation and rejects unexpected wire formats. WEB login omits binary-only fingerprint/experiment fields. Mock WebSocket tests cover INIT → CAPTCHA → auth, token login, large IDs and consent. No live session has been used to claim compatibility.


Contacts checkpoint: the Telegram-compatible contact response now contains contact rows as well as user objects. After Android READ_CONTACTS permission, names are matched locally by normalized international phone numbers; the phonebook is not uploaded. Local contact removal hides a contact in the client's own store and avoids deleting Android phonebook records. Profile metadata converts local adapter IDs back to actual MAX IDs. The supplied APK's cg4 contact options confirm the BOT flag. Live contact/presence acceptance remains pending.
