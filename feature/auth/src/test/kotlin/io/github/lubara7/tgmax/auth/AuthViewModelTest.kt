package io.github.lubara7.tgmax.auth

import androidx.lifecycle.ViewModelStore
import io.github.lubara7.tgmax.domain.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = mutableListOf<AuthViewModel>()
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() {
        models.forEach { model -> ViewModelStore().apply { put("auth", model); clear() } }
        Dispatchers.resetMain()
    }
    private fun model(repository: FakeRepository): AuthViewModel =
        AuthViewModel(repository, initialRegion = "GB", nowMillis = { 1_000 }).also { models += it }

    @Test fun `invalid phone never requests a code`() = authTest {
        val repository = FakeRepository()
        val vm = model(repository)
        runCurrent()
        vm.setPhone("123")
        vm.requestCode()
        assertEquals(AuthError.INVALID_PHONE, vm.state.value.error)
        assertEquals(0, repository.requests)
    }

    @Test fun `duplicate taps trigger one request and successful verification clears code`() = authTest {
        val repository = FakeRepository().apply { requestGate = CompletableDeferred() }
        val vm = model(repository)
        runCurrent()
        vm.setPhone("020 7946 0018")
        vm.requestCode()
        vm.requestCode()
        runCurrent()
        assertEquals(1, repository.requests)
        assertTrue(vm.state.value.busy)
        repository.requestGate!!.complete(Unit)
        runCurrent()
        assertNotNull(vm.state.value.challenge)
        vm.setCode("12345")
        vm.verifyCode()
        runCurrent()
        assertTrue(vm.state.value.authenticated)
        assertEquals("", vm.state.value.codeInput)
        assertEquals("", vm.state.value.phoneInput)
        assertNull(vm.state.value.challenge)
    }

    @Test fun `expired code remains recoverable and does not authenticate`() = authTest {
        val repository = FakeRepository().apply { verificationError = AuthError.EXPIRED_CODE }
        val vm = model(repository)
        runCurrent()
        vm.setPhone("+442079460018")
        vm.requestCode()
        runCurrent()
        vm.setCode("12345")
        vm.verifyCode()
        runCurrent()
        assertFalse(vm.state.value.authenticated)
        assertEquals(AuthError.EXPIRED_CODE, vm.state.value.error)
        assertEquals("", vm.state.value.codeInput)
        assertNotNull(vm.state.value.challenge)
    }

    @Test fun `returning to phone retains resend cooldown`() = authTest {
        val repository = FakeRepository()
        val vm = model(repository)
        runCurrent()
        vm.setPhone("+442079460018")
        vm.requestCode()
        runCurrent()
        vm.editPhone()
        assertEquals(30, vm.state.value.retrySeconds)
        vm.requestCode()
        runCurrent()
        assertEquals(1, repository.requests)
    }

    @Test fun `saved session resumes without asking for a code`() = authTest {
        val repository = FakeRepository().apply { restoredSession = Session("user", "secret") }
        val vm = model(repository)
        runCurrent()
        assertTrue(vm.state.value.authenticated)
        assertFalse(vm.state.value.restoring)
        assertEquals(0, repository.requests)
    }

    private fun authTest(block: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        try { block() }
        finally {
            // Dispose before runTest drains its scheduler; the ViewModel owns a repeating timer.
            models.forEach { model -> ViewModelStore().apply { put("auth", model); clear() } }
            models.clear()
            runCurrent()
        }
    }

    private class FakeRepository : AuthRepository {
        var requests = 0
        var requestGate: CompletableDeferred<Unit>? = null
        var verificationError: AuthError? = null
        var restoredSession: Session? = null
        override suspend fun requestCode(phone: PhoneNumber): AuthResult<AuthChallenge> {
            requests++
            requestGate?.await()
            return AuthResult.Success(AuthChallenge("id", phone, 5, 300_000, 31_000))
        }
        override suspend fun resendCode(challenge: AuthChallenge) = AuthResult.Success(challenge)
        override suspend fun verifyCode(challenge: AuthChallenge, code: String): AuthResult<Session> =
            verificationError?.let { AuthResult.Failure(it) } ?: AuthResult.Success(Session("user", "secret"))
        override suspend fun restoreSession() = AuthResult.Success(restoredSession)
        override suspend fun signOut() = AuthResult.Success(Unit)
    }
}
