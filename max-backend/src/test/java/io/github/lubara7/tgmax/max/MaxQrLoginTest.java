package io.github.lubara7.tgmax.max;

import org.junit.Test;
import static org.junit.Assert.*;

public class MaxQrLoginTest {
    @Test public void serverLifetimeAndPollingIntervalRemainMilliseconds() {
        MaxQrLogin.Params p = new MaxQrLogin.Params(Maps.of("trackId", "synthetic-track", "qrLink",
                "https://web.max.ru/qr?token=synthetic", "ttl", 120000L, "pollingInterval", 3000));
        assertEquals(120000L, p.ttlMillis);
        assertEquals(3000L, p.pollingMillis);
        assertFalse(new MaxQrLogin.Status(Maps.of("status", Maps.of("ttl", 90000L))).loginAvailable);
        assertTrue(new MaxQrLogin.Status(Maps.of("status", Maps.of("loginAvailable", true, "ttl", 0))).loginAvailable);
    }

    @Test public void wrongLoginSchemeUnboundedLifetimeAndMissingStatusAreRejected() {
        for (String link : new String[] {"tg://login?token=synthetic", "javascript:alert(1)", "https://example.com/qr"}) {
            try {
                new MaxQrLogin.Params(Maps.of("trackId", "synthetic", "qrLink", link, "ttl", 120000, "pollingInterval", 1000));
                fail("Unexpected login scheme");
            } catch (MaxException expected) { }
        }
        try {
            new MaxQrLogin.Params(Maps.of("trackId", "synthetic", "qrLink", "https://max.ru/qr", "ttl", Long.MAX_VALUE, "pollingInterval", 1000));
            fail("Unbounded QR lifetime");
        } catch (MaxException expected) { }
        try { new MaxQrLogin.Status(Maps.of()); fail("Missing QR status"); }
        catch (MaxException expected) { }
    }
}
