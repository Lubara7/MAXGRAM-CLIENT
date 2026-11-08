package io.github.lubara7.tgmax.domain

/** Application contract, not an assertion about the wire format of MAX Open Client API. */
class AuthChallenge(
    val id: String,
    val phone: PhoneNumber,
    val codeLength: Int,
    val expiresAtEpochMillis: Long,
    val resendAtEpochMillis: Long,
) {
    init { require(codeLength in 1..12) }
    override fun toString(): String = "AuthChallenge([redacted])"
}
