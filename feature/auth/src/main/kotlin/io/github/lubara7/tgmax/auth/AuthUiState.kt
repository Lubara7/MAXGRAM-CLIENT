package io.github.lubara7.tgmax.auth

import io.github.lubara7.tgmax.domain.AuthChallenge
import io.github.lubara7.tgmax.domain.AuthError

data class AuthUiState(
    val restoring: Boolean = true,
    val busy: Boolean = false,
    val phoneInput: String = "",
    val region: String = "US",
    val codeInput: String = "",
    val challenge: AuthChallenge? = null,
    val authenticated: Boolean = false,
    val error: AuthError? = null,
    val retryAtEpochMillis: Long = 0,
    val nowMillis: Long = 0,
) {
    override fun toString(): String =
        "AuthUiState(restoring=$restoring, busy=$busy, authenticated=$authenticated, sensitiveFields=[redacted])"

    val retrySeconds: Int
        get() = ((maxOf(retryAtEpochMillis, challenge?.resendAtEpochMillis ?: 0) - nowMillis + 999) / 1_000)
            .coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
}
