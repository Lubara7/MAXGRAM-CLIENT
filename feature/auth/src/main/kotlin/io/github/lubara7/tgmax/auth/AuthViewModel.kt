package io.github.lubara7.tgmax.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.lubara7.tgmax.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AuthViewModel(
    private val repository: AuthRepository,
    initialRegion: String = PhoneNumberNormalizer.defaultRegion(),
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AuthUiState(region = initialRegion, nowMillis = nowMillis()))
    val state = mutableState.asStateFlow()

    init {
        execute {
            when (val result = repository.restoreSession()) {
                is AuthResult.Success -> mutableState.update {
                    it.copy(restoring = false, authenticated = result.value != null)
                }
                is AuthResult.Failure -> fail(result)
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                if (state.value.challenge != null || state.value.retryAtEpochMillis > 0) {
                    mutableState.update { it.copy(nowMillis = nowMillis()) }
                }
            }
        }
    }

    fun setPhone(value: String) {
        if (!state.value.busy) mutableState.update { it.copy(phoneInput = value.take(40), error = null) }
    }

    fun setRegion(region: String) {
        if (!state.value.busy && region in PhoneNumberNormalizer.regions)
            mutableState.update { it.copy(region = region, error = null) }
    }

    fun setCode(value: String) {
        if (!state.value.busy) mutableState.update {
            it.copy(codeInput = value.filter { c -> c in '0'..'9' }.take(it.challenge?.codeLength ?: 12), error = null)
        }
    }

    fun requestCode() {
        if (state.value.busy || state.value.retrySeconds > 0) return
        val phone = PhoneNumberNormalizer.parse(state.value.phoneInput, state.value.region)
        if (phone == null) {
            mutableState.update { it.copy(error = AuthError.INVALID_PHONE) }
            return
        }
        execute {
            when (val result = repository.requestCode(phone)) {
                is AuthResult.Success -> mutableState.update {
                    it.copy(challenge = result.value, codeInput = "", error = null, nowMillis = nowMillis(), retryAtEpochMillis = 0)
                }
                is AuthResult.Failure -> fail(result)
            }
        }
    }

    fun verifyCode() {
        val current = state.value
        val challenge = current.challenge ?: return
        if (current.busy) return
        execute {
            when (val result = repository.verifyCode(challenge, current.codeInput)) {
                is AuthResult.Success -> mutableState.update {
                    it.copy(authenticated = true, challenge = null, codeInput = "", phoneInput = "", error = null,
                        retryAtEpochMillis = 0)
                }
                is AuthResult.Failure -> {
                    fail(result)
                    mutableState.update { it.copy(codeInput = "") }
                }
            }
        }
    }

    fun resendCode() {
        val current = state.value
        val challenge = current.challenge ?: return
        if (current.busy || current.retrySeconds > 0) return
        execute {
            when (val result = repository.resendCode(challenge)) {
                is AuthResult.Success -> mutableState.update {
                    it.copy(challenge = result.value, codeInput = "", error = null, retryAtEpochMillis = 0, nowMillis = nowMillis())
                }
                is AuthResult.Failure -> fail(result)
            }
        }
    }

    fun editPhone() {
        if (!state.value.busy) mutableState.update {
            it.copy(challenge = null, codeInput = "", error = null,
                retryAtEpochMillis = maxOf(it.retryAtEpochMillis, it.challenge?.resendAtEpochMillis ?: 0))
        }
    }

    fun signOut() {
        execute {
            val result = repository.signOut()
            if (result is AuthResult.Failure && result.error == AuthError.STORAGE) {
                fail(result)
                return@execute
            }
            mutableState.update {
                AuthUiState(restoring = false, region = it.region, nowMillis = nowMillis(), busy = true,
                    error = (result as? AuthResult.Failure)?.error)
            }
        }
    }

    private fun fail(result: AuthResult.Failure) {
        mutableState.update {
            it.copy(restoring = false, error = result.error, nowMillis = nowMillis(),
                retryAtEpochMillis = result.retryAtEpochMillis ?: it.retryAtEpochMillis)
        }
    }

    private fun execute(block: suspend () -> Unit) {
        if (state.value.busy) return
        mutableState.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { fail(AuthResult.Failure(AuthError.UNKNOWN)) }
            finally { mutableState.update { it.copy(busy = false, restoring = false) } }
        }
    }
}
