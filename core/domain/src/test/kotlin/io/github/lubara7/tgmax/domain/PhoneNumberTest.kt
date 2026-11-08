package io.github.lubara7.tgmax.domain

import org.junit.Assert.*
import org.junit.Test

class PhoneNumberTest {
    @Test fun `E164 requires international prefix and ASCII digits`() {
        listOf("79991234567", "+0123456789", "+12", "+1234567890123456", "+7 999 1234567", "+٧٩٩٩١٢٣٤٥٦٧")
            .forEach { assertNull(it, PhoneNumber.fromE164(it)) }
        assertEquals("+442079460018", PhoneNumber.fromE164("+442079460018")?.e164)
    }

    @Test fun `credentials and phone are redacted from diagnostics`() {
        assertFalse(Session("user", "secret-token").toString().contains("secret-token"))
        assertFalse(PhoneNumber.fromE164("+442079460018").toString().contains("442079460018"))
    }
}
