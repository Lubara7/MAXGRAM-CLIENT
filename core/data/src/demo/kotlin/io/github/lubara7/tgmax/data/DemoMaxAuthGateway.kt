package io.github.lubara7.tgmax.data

import io.github.lubara7.tgmax.domain.*
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** This implementation exists only in demo artifacts. No messages are sent. */
class DemoMaxAuthGateway(private val nowMillis: () -> Long = System::currentTimeMillis) : MaxAuthGateway {
    private val mutex = Mutex()
    private var active: AuthChallenge? = null
    private var failedAttempts = 0

    override suspend fun requestCode(phone: PhoneNumber): AuthResult<AuthChallenge> = mutex.withLock {
        val previous = active
        if (previous != null && nowMillis() < previous.resendAtEpochMillis) {
            return@withLock AuthResult.Failure(AuthError.RATE_LIMITED, previous.resendAtEpochMillis)
        }
        issue(phone)
    }

    override suspend fun resendCode(challenge: AuthChallenge): AuthResult<AuthChallenge> = mutex.withLock {
        val current = active
        when {
            current == null || current.id != challenge.id -> AuthResult.Failure(AuthError.EXPIRED_CODE)
            nowMillis() < current.resendAtEpochMillis ->
                AuthResult.Failure(AuthError.RATE_LIMITED, current.resendAtEpochMillis)
            else -> issue(current.phone)
        }
    }

    override suspend fun verifyCode(challenge: AuthChallenge, code: String): AuthResult<Session> = mutex.withLock {
        val current = active
        when {
            current == null || current.id != challenge.id || nowMillis() >= current.expiresAtEpochMillis || failedAttempts >= 5 ->
                AuthResult.Failure(AuthError.EXPIRED_CODE)
            code != DEMO_CODE -> {
                failedAttempts++
                if (failedAttempts >= 5) {
                    AuthResult.Failure(AuthError.EXPIRED_CODE)
                } else AuthResult.Failure(AuthError.INVALID_CODE)
            }
            else -> {
                active = null // A code cannot be replayed after success.
                AuthResult.Success(Session("demo-user", "demo-session-${UUID.randomUUID()}"))
            }
        }
    }

    override suspend fun validateSession(session: Session): AuthResult<Session> =
        if (session.userId == "demo-user" && session.credential.startsWith("demo-session-"))
            AuthResult.Success(session) else AuthResult.Failure(AuthError.SESSION_EXPIRED)

    override suspend fun signOut(session: Session): AuthResult<Unit> {
        mutex.withLock { active = null }
        return AuthResult.Success(Unit)
    }

    private fun issue(phone: PhoneNumber): AuthResult<AuthChallenge> {
        val now = nowMillis()
        failedAttempts = 0
        return AuthChallenge(UUID.randomUUID().toString(), phone, DEMO_CODE.length, now + 300_000, now + 30_000)
            .also { active = it }.let { AuthResult.Success(it) }
    }

    companion object { const val DEMO_CODE = "12345" }
}
