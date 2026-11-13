package io.github.lubara7.tgmax.max;

import org.junit.Test;
import static org.junit.Assert.*;

public class MaxCallOutTest {
    @Test public void ttlAndPollingIntervalStayInMilliseconds() {
        MaxCallOut.Params params = new MaxCallOut.Params(Maps.of("trackId", "synthetic-track", "ttl", 120000, "pollingInterval", 1500));
        assertEquals(120000, params.ttlMillis);
        assertEquals(1500, params.pollingIntervalMillis);
    }
    @Test public void expiryDoesNotRequireACallNumberAndDialerInputIsRestricted() {
        assertEquals(3, new MaxCallOut.Check(Maps.of("status", 3)).status);
        assertEquals(2, new MaxCallOut.Check(Maps.of("status", 2)).status);
        assertEquals("+19995550123", new MaxCallOut.Check(Maps.of("status", 1, "callNumber", "19995550123")).callNumber);
        try { new MaxCallOut.Check(Maps.of("status", 1, "callNumber", "*123#")); fail(); }
        catch (MaxException expected) { assertEquals("MAX_CALL_NUMBER_INVALID", expected.code); }
        try { new MaxCallOut.Params(Maps.of("trackId", "synthetic", "ttl", 0, "pollingInterval", 1000)); fail(); }
        catch (MaxException expected) { assertEquals("MAX_CALL_PARAMS_INVALID", expected.code); }
    }
}
