package io.github.lubara7.tgmax.data

import io.github.lubara7.tgmax.domain.*
import java.io.IOException
import java.security.GeneralSecurityException
import kotlinx.coroutines.CancellationException

class MaxAuthRepository(
    private val gateway: MaxAuthGateway,
    private val store: SessionStore,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : AuthRepository {
    override suspend fun requestCode(phone: PhoneNumber): AuthResult<AuthChallenge> = guarded {
        gateway.requestCode(phone)
    }

    override suspend fun resendCode(challenge: AuthChallenge): AuthResult<AuthChallenge> = guarded {
        if (nowMillis() < challenge.resendAtEpochMillis) {
            AuthResult.Failure(AuthError.RATE_LIMITED, challenge.resendAtEpochMillis)
        } else gateway.resendCode(challenge)
    }

    override suspend fun verifyCode(challenge: AuthChallenge, code: String): AuthResult<Session> = guarded {
        when {
            nowMillis() >= challenge.expiresAtEpochMillis -> AuthResult.Failure(AuthError.EXPIRED_CODE)
            code.length != challenge.codeLength || code.any { it !in '0'..'9' } ->
                AuthResult.Failure(AuthError.INVALID_CODE)
            else -> when (val result = gateway.verifyCode(challenge, code)) {
                is AuthResult.Success -> {
                    store.write(result.value)
                    result
                }
                is AuthResult.Failure -> result
            }
        }
    }

    override suspend fun restoreSession(): AuthResult<Session?> = guarded {
        val saved = try {
            store.read()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            store.clear()
            return@guarded AuthResult.Failure(AuthError.STORAGE)
        }
        if (saved == null) return@guarded AuthResult.Success(null)
        when (val result = gateway.validateSession(saved)) {
            is AuthResult.Success -> result.also { store.write(it.value) }
            is AuthResult.Failure -> result.also {
                if (it.error == AuthError.SESSION_EXPIRED) store.clear()
            }
        }
    }

    override suspend fun signOut(): AuthResult<Unit> = guarded {
        val saved = try {
            store.read()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            store.clear()
            return@guarded AuthResult.Failure(AuthError.STORAGE)
        }
        // Local logout does not depend on the availability of remote session revocation.
        store.clear()
        if (saved == null) AuthResult.Success(Unit) else gateway.signOut(saved)
    }

    private suspend fun <T> guarded(block: suspend () -> AuthResult<T>): AuthResult<T> = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (_: GeneralSecurityException) {
        AuthResult.Failure(AuthError.STORAGE)
    } catch (_: IOException) {
        // Gateway implementations map transport errors before returning to the repository.
        AuthResult.Failure(AuthError.STORAGE)
    } catch (_: Exception) {
        AuthResult.Failure(AuthError.UNKNOWN)
    }
}
