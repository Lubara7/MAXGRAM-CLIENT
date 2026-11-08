package io.github.lubara7.tgmax.auth

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import io.github.lubara7.tgmax.domain.PhoneNumber
import java.util.Locale

object PhoneNumberNormalizer {
    private val phoneUtil = PhoneNumberUtil.getInstance()
    val regions: List<String> get() = phoneUtil.supportedRegions.sorted()
    fun callingCode(region: String): Int = phoneUtil.getCountryCodeForRegion(region)
    fun defaultRegion(): String = Locale.getDefault().country.takeIf { it in regions } ?: "US"

    fun parse(input: String, region: String): PhoneNumber? = try {
        // Reject extensions instead of silently discarding information the user entered.
        val parsed = phoneUtil.parse(input, region)
        if (!phoneUtil.isValidNumber(parsed) || parsed.hasExtension()) null
        else PhoneNumber.fromE164(phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164))
    } catch (_: NumberParseException) { null }
}
