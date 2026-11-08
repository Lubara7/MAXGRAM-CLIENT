package io.github.lubara7.tgmax.data

import io.github.lubara7.tgmax.domain.*

/** Replace only after obtaining the App-ID and verified Open Client API integration details. */
class OpenClientAuthGateway(val config: MaxClientConfig) : MaxAuthGateway {
    private fun unavailable() = AuthResult.Failure(AuthError.INTEGRATION_UNAVAILABLE)
    override suspend fun requestCode(phone: PhoneNumber): AuthResult<AuthChallenge> = unavailable()
    override suspend fun resendCode(challenge: AuthChallenge): AuthResult<AuthChallenge> = unavailable()
    override suspend fun verifyCode(challenge: AuthChallenge, code: String): AuthResult<Session> = unavailable()
    override suspend fun validateSession(session: Session): AuthResult<Session> = unavailable()
    override suspend fun signOut(session: Session): AuthResult<Unit> = unavailable()
}
