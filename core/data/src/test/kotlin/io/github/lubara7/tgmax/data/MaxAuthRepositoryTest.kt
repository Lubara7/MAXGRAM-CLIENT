package io.github.lubara7.tgmax.data

import io.github.lubara7.tgmax.domain.*
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class MaxAuthRepositoryTest {
    private val phone = PhoneNumber.fromE164("+442079460018")!!
    private val challenge = AuthChallenge("challenge", phone, 5, 10_000, 1_000)
    private val session = Session("user", "credential")

    @Test fun `cooldown is enforced before calling gateway`() = runTest {
        val gateway = StubGateway(session)
        val result = MaxAuthRepository(gateway, MemoryStore(), { 500 }).resendCode(challenge)
        assertEquals(AuthError.RATE_LIMITED, (result as AuthResult.Failure).error)
        assertEquals(0, gateway.resends)
    }

    @Test fun `authentication succeeds only after session is saved`() = runTest {
        val store = MemoryStore()
        val repository = MaxAuthRepository(StubGateway(session), store, { 2_000 })
        assertTrue(repository.verifyCode(challenge, "12345") is AuthResult.Success)
        assertSame(session, store.value)
        store.failWrite = true
        val result = repository.verifyCode(challenge, "12345") as AuthResult.Failure
        assertEquals(AuthError.STORAGE, result.error)
    }

    @Test fun `invalid length and expired codes do not reach gateway`() = runTest {
        val gateway = StubGateway(session)
        val repository = MaxAuthRepository(gateway, MemoryStore(), { 2_000 })
        assertEquals(AuthError.INVALID_CODE, (repository.verifyCode(challenge, "12") as AuthResult.Failure).error)
        assertEquals(AuthError.EXPIRED_CODE,
            (MaxAuthRepository(gateway, MemoryStore(), { 10_000 }).verifyCode(challenge, "12345") as AuthResult.Failure).error)
        assertEquals(0, gateway.verifications)
    }

    @Test fun `expired sessions are cleared but transient failures preserve them`() = runTest {
        val store = MemoryStore().apply { value = session }
        val gateway = StubGateway(session).apply { validationError = AuthError.NETWORK }
        val repository = MaxAuthRepository(gateway, store)
        assertTrue(repository.restoreSession() is AuthResult.Failure)
        assertSame(session, store.value)
        gateway.validationError = AuthError.SESSION_EXPIRED
        repository.restoreSession()
        assertNull(store.value)
    }

    @Test fun `sign out clears local session even if remote revocation fails`() = runTest {
        val store = MemoryStore().apply { value = session }
        val repository = MaxAuthRepository(StubGateway(session), store)
        assertTrue(repository.signOut() is AuthResult.Failure)
        assertNull(store.value)
    }

    @Test fun `cancellation is propagated`() = runTest {
        val gateway = StubGateway(session).apply { cancel = true }
        try {
            MaxAuthRepository(gateway, MemoryStore()).requestCode(phone)
            fail("Cancellation was swallowed")
        } catch (_: CancellationException) { /* expected */ }
    }

    private class MemoryStore : SessionStore {
        var value: Session? = null
        var failWrite = false
        override suspend fun read() = value
        override suspend fun write(session: Session) {
            if (failWrite) throw IOException("Write failed")
            value = session
        }
        override suspend fun clear() { value = null }
    }

    private class StubGateway(private val session: Session) : MaxAuthGateway {
        var resends = 0
        var verifications = 0
        var validationError: AuthError? = null
        var cancel = false
        override suspend fun requestCode(phone: PhoneNumber): AuthResult<AuthChallenge> {
            if (cancel) throw CancellationException()
            return AuthResult.Success(AuthChallenge("id", phone, 5, 10_000, 1_000))
        }
        override suspend fun resendCode(challenge: AuthChallenge): AuthResult<AuthChallenge> {
            resends++
            return AuthResult.Success(challenge)
        }
        override suspend fun verifyCode(challenge: AuthChallenge, code: String): AuthResult<Session> {
            verifications++
            return AuthResult.Success(session)
        }
        override suspend fun validateSession(session: Session): AuthResult<Session> =
            validationError?.let { AuthResult.Failure(it) } ?: AuthResult.Success(session)
        override suspend fun signOut(session: Session): AuthResult<Unit> = AuthResult.Failure(AuthError.NETWORK)
    }
}
