package io.github.lubara7.tgmax.data

import io.github.lubara7.tgmax.domain.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class DemoMaxAuthGatewayTest {
    private val phone = PhoneNumber.fromE164("+442079460018")!!

    @Test fun `test code is single use and never authenticates with wrong code`() = runTest {
        val gateway = DemoMaxAuthGateway { 1_000 }
        val challenge = (gateway.requestCode(phone) as AuthResult.Success).value
        assertEquals(AuthError.INVALID_CODE, (gateway.verifyCode(challenge, "00000") as AuthResult.Failure).error)
        assertTrue(gateway.verifyCode(challenge, DemoMaxAuthGateway.DEMO_CODE) is AuthResult.Success)
        assertEquals(AuthError.EXPIRED_CODE,
            (gateway.verifyCode(challenge, DemoMaxAuthGateway.DEMO_CODE) as AuthResult.Failure).error)
    }

    @Test fun `resending invalidates old challenge and respects cooldown`() = runTest {
        var clock = 1_000L
        val gateway = DemoMaxAuthGateway { clock }
        val original = (gateway.requestCode(phone) as AuthResult.Success).value
        assertTrue(gateway.resendCode(original) is AuthResult.Failure)
        clock += 30_000
        val replacement = (gateway.resendCode(original) as AuthResult.Success).value
        assertNotEquals(original.id, replacement.id)
        assertEquals(AuthError.EXPIRED_CODE,
            (gateway.verifyCode(original, DemoMaxAuthGateway.DEMO_CODE) as AuthResult.Failure).error)
        assertTrue(gateway.verifyCode(replacement, DemoMaxAuthGateway.DEMO_CODE) is AuthResult.Success)
    }

    @Test fun `expired code cannot authenticate`() = runTest {
        var clock = 1_000L
        val gateway = DemoMaxAuthGateway { clock }
        val challenge = (gateway.requestCode(phone) as AuthResult.Success).value
        clock = challenge.expiresAtEpochMillis
        assertEquals(AuthError.EXPIRED_CODE,
            (gateway.verifyCode(challenge, DemoMaxAuthGateway.DEMO_CODE) as AuthResult.Failure).error)
    }

    @Test fun `attempt limit rejects even correct code but resend recovers`() = runTest {
        var clock = 1_000L
        val gateway = DemoMaxAuthGateway { clock }
        val challenge = (gateway.requestCode(phone) as AuthResult.Success).value
        repeat(5) { gateway.verifyCode(challenge, "00000") }
        assertEquals(AuthError.EXPIRED_CODE,
            (gateway.verifyCode(challenge, DemoMaxAuthGateway.DEMO_CODE) as AuthResult.Failure).error)
        clock += 30_000
        val replacement = (gateway.resendCode(challenge) as AuthResult.Success).value
        assertTrue(gateway.verifyCode(replacement, DemoMaxAuthGateway.DEMO_CODE) is AuthResult.Success)
    }
}
