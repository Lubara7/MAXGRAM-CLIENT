package io.github.lubara7.tgmax.domain

/** E.164 syntax; regional validity is checked at the UI boundary with libphonenumber. */
class PhoneNumber private constructor(val e164: String) {
    override fun toString(): String = "PhoneNumber([redacted])"
    override fun equals(other: Any?): Boolean = other is PhoneNumber && e164 == other.e164
    override fun hashCode(): Int = e164.hashCode()

    companion object {
        fun fromE164(value: String): PhoneNumber? =
            value.takeIf { it.matches(Regex("\\+[1-9][0-9]{7,14}")) }?.let(::PhoneNumber)
    }
}
