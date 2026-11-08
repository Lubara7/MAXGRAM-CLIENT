# Nagram / MAX integration

The application UI is prepared from pinned NagramXF, not recreated in Compose. `prepare_nagram_client.py` clones the supplied upstream checkout, checks its revision, installs the Java MAX backend and overlays, guards Java/JNI MTProto entry points, restricts language selection and disables Telegram API credentials, upstream update checks, inherited Firebase initialization and Telegram push registration.

The original JNI declarations remain for native registration compatibility; guarded Java wrappers do not invoke the MTProto network originals. A full native dependency removal and full egress audit are still required before describing the client as a completed Telegram-backend removal. FileLoader, stories/call controllers and unsupported settings need dedicated MAX replacements.

`TgMaxRuntime` maps selected TLRPC request objects to MAX operations. TLRPC models are currently in-memory UI compatibility models, never serialized as MAX wire packets. MAX server IDs are preserved through `StableIds`; the initial persistent JSON index should migrate to a transactional database before large-scale use.

MAX password verification uses a server trackId and opcode 115, following Komet AccountModule.checkPassword. The existing Nagram password page is reused without Telegram SRP. Password recovery/email and outgoing auth-call challenges are not implemented. The UI does not synthesize successful authorization for these steps.

API support today: phone OTP/password login, account snapshot, dialogs/history, text/reply/edit/delete/read, cached contact/profile conversion and known channel join/leave. Unsupported request types return explicit errors. Reconnect is demand-driven; full lossless MAX gap recovery is still a task. Existing Nagram UI may expose unsupported controls in this alpha.

Tests use synthetic packets and MockWebServer; no credentials or captured user traffic. The quick API-shape check compiles overlay files against extracted pinned members and narrow Android/messenger API shapes. Only a full `assembleNormalDebug` validates the complete Android compilation; only installation/execution validates runtime behavior.

The source build is ARM64 when NATIVE_TARGET=arm64-v8a. The experimental APK uses Android debug signing, not a secret production signing key. Release publication is handled by the Nagram release workflow after successful build and signature verification. No auth-harness APK is selected by that workflow.
