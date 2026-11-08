package io.github.lubara7.tgmax.domain

interface AuthRepository {
    suspend fun requestCode(phone: PhoneNumber): AuthResult<AuthChallenge>
    suspend fun resendCode(challenge: AuthChallenge): AuthResult<AuthChallenge>
    suspend fun verifyCode(challenge: AuthChallenge, code: String): AuthResult<Session>
    suspend fun restoreSession(): AuthResult<Session?>
    suspend fun signOut(): AuthResult<Unit>
}
