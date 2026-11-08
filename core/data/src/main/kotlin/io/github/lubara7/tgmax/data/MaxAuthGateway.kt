package io.github.lubara7.tgmax.data

import io.github.lubara7.tgmax.domain.*

/** On-device adapter boundary. Method names here are not MAX HTTP endpoints. */
interface MaxAuthGateway {
    suspend fun requestCode(phone: PhoneNumber): AuthResult<AuthChallenge>
    suspend fun resendCode(challenge: AuthChallenge): AuthResult<AuthChallenge>
    suspend fun verifyCode(challenge: AuthChallenge, code: String): AuthResult<Session>
    suspend fun validateSession(session: Session): AuthResult<Session>
    suspend fun signOut(session: Session): AuthResult<Unit>
}
