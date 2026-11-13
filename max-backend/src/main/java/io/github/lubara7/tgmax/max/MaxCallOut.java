package io.github.lubara7.tgmax.max;

import java.util.Map;

public final class MaxCallOut {
    private MaxCallOut() { }

    public static final class Params {
        public final String trackId;
        public final long ttlMillis;
        public final long pollingIntervalMillis;

        public Params(Object raw) {
            Map<String, Object> data = Maps.object(raw);
            trackId = Maps.string(data.get("trackId"));
            ttlMillis = Maps.number(data.get("ttl"));
            pollingIntervalMillis = Maps.number(data.get("pollingInterval"));
            if (trackId.isEmpty() || ttlMillis <= 0 || ttlMillis > 86_400_000 || pollingIntervalMillis <= 0 ||
                    pollingIntervalMillis > 86_400_000) throw new MaxException("MAX_CALL_PARAMS_INVALID");
        }
    }

    public static final class Check {
        public final int status;
        public final String callNumber;

        public Check(Map<String, Object> data) {
            long value = Maps.number(data.get("status"), -1);
            if (value < 0 || value > 3) throw new MaxException("MAX_CALL_STATUS_INVALID");
            status = (int) value;
            String number = Maps.string(data.get("callNumber"));
            callNumber = number.matches("\\+?[1-9][0-9]{6,14}") ? (number.startsWith("+") ? number : "+" + number) : "";
            if (status == 1 && callNumber.isEmpty()) throw new MaxException("MAX_CALL_NUMBER_INVALID");
        }
    }
}
