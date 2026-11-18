package io.github.lubara7.tgmax.max;

import java.util.Map;

/** MAX status codes are server privacy buckets, not client time thresholds. */
public final class MaxPresence {
    public enum State { OFFLINE, ONLINE, RECENTLY, LONG_AGO }
    // Supplied MAX 26.19.2: PmsProperties presence-ttl default is 300 seconds.
    public static final long CACHE_TTL_MILLIS = 300_000;
    public final long seenSeconds;
    private final int status;
    private final long observedAt;

    public MaxPresence(Map<String,Object> data, long elapsedMillis) {
        long seen = Maps.number(data.get("seen"), 0);
        seenSeconds = seen > 0 && seen <= Integer.MAX_VALUE ? seen : 0;
        long code = Maps.number(data.get("status"), 0);
        status = code >= 0 && code <= 3 ? (int) code : 3;
        observedAt = elapsedMillis;
    }

    public State state(long elapsedMillis, boolean blocked) {
        if (blocked) return State.LONG_AGO;
        if (status == 2) return State.RECENTLY;
        if (status == 3) return State.LONG_AGO;
        if (status == 1 && remainingMillis(elapsedMillis) > 0) return State.ONLINE;
        return seenSeconds == 0 ? State.LONG_AGO : State.OFFLINE;
    }

    public long remainingMillis(long elapsedMillis) {
        long age = Math.max(0, elapsedMillis - observedAt);
        return age >= CACHE_TTL_MILLIS ? 0 : CACHE_TTL_MILLIS - age;
    }
}
