package io.github.lubara7.tgmax.max;

import org.junit.Test;
import static org.junit.Assert.*;

public class MaxAuthChallengeTest {
    @Test public void smsKeepsServerCodeLengthAndResendDelay() {
        MaxAuthChallenge challenge = new MaxAuthChallenge(Maps.of("token", "synthetic-otp", "codeLength", 5, "altActionDuration", 90500));
        assertNull(challenge.call);
        assertEquals(5, challenge.codeLength);
        assertEquals(91, challenge.resendSeconds);
    }
    @Test public void callDoesNotRequireAnSmsCodeLength() {
        MaxAuthChallenge challenge = new MaxAuthChallenge(Maps.of("codeLength", 0, "callOutParams",
                Maps.of("trackId", "synthetic-call", "ttl", 120000, "pollingInterval", 1500)));
        assertNotNull(challenge.call);
        assertEquals("synthetic-call", challenge.call.trackId);
        assertEquals(0, challenge.codeLength);
    }
    @Test public void malformedOptionalCallDoesNotDiscardAValidSmsToken() {
        MaxAuthChallenge challenge = new MaxAuthChallenge(Maps.of("token", "synthetic-otp", "codeLength", 6, "callOutParams", Maps.of()));
        assertNull(challenge.call);
        assertEquals("synthetic-otp", challenge.token);
    }
    @Test public void missingTokenOrInvalidSmsLengthCannotOpenAnOtpScreen() {
        try { new MaxAuthChallenge(Maps.of("codeLength", 6)); fail(); }
        catch (MaxException expected) { assertEquals("MAX_AUTH_TOKEN_MISSING", expected.code); }
        try { new MaxAuthChallenge(Maps.of("token", "synthetic", "codeLength", 0)); fail(); }
        catch (MaxException expected) { assertEquals("MAX_CODE_LENGTH_INVALID", expected.code); }
    }
}
