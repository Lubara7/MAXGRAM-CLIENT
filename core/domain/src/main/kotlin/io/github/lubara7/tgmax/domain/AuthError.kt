package io.github.lubara7.tgmax.domain

enum class AuthError {
    INVALID_PHONE, INVALID_CODE, EXPIRED_CODE, RATE_LIMITED, NETWORK,
    INTEGRATION_UNAVAILABLE, SESSION_EXPIRED, STORAGE, UNSUPPORTED_CHALLENGE, UNKNOWN,
}

sealed interface AuthResult<out T> {
    data class Success<T>(val value: T) : AuthResult<T>
    data class Failure(val error: AuthError, val retryAtEpochMillis: Long? = null) : AuthResult<Nothing>
}
