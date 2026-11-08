package io.github.lubara7.tgmax.domain

class Session(val userId: String, val credential: String) {
    init { require(userId.isNotBlank() && credential.isNotBlank()) }
    override fun toString(): String = "Session([redacted])"
}
