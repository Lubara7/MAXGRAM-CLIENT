package io.github.lubara7.tgmax.max;

import java.net.URI;
import java.util.Map;

public final class MaxQrLogin {
    private MaxQrLogin() {}

    public static final class Params {
        public final String trackId, qrLink;
        public final long ttlMillis, pollingMillis;

        public Params(Map<String, Object> data) {
            trackId = Maps.string(data.get("trackId"));
            qrLink = Maps.string(data.get("qrLink"));
            ttlMillis = Maps.number(data.get("ttl"), 0);
            pollingMillis = Maps.number(data.get("pollingInterval"), 0);
            if (trackId.isEmpty() || trackId.length() > 2048 || !validLink(qrLink) ||
                    ttlMillis <= 0 || ttlMillis > 86_400_000 || pollingMillis < 250 || pollingMillis > 60_000)
                throw new MaxException("MAX_INVALID_QR_RESPONSE");
        }
    }

    public static final class Status {
        public final boolean loginAvailable;
        public final long ttlMillis;

        public Status(Map<String, Object> data) {
            Map<String, Object> status = Maps.object(data.get("status"));
            if (!(data.get("status") instanceof Map) || (status.get("loginAvailable") != null && !(status.get("loginAvailable") instanceof Boolean)))
                throw new MaxException("MAX_INVALID_QR_STATUS");
            loginAvailable = Boolean.TRUE.equals(status.get("loginAvailable"));
            ttlMillis = Maps.number(status.get("ttl"), 0);
            if (ttlMillis < 0 || ttlMillis > 86_400_000) throw new MaxException("MAX_INVALID_QR_STATUS");
        }
    }

    private static boolean validLink(String value) {
        if (value.isEmpty() || value.length() > 4096 || value.chars().anyMatch(c -> Character.isISOControl(c))) return false;
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            if ("max".equals(scheme) || "oneme".equals(scheme)) return true;
            String host = uri.getHost();
            return "https".equals(scheme) && uri.getUserInfo() == null && host != null &&
                    (host.equals("max.ru") || host.endsWith(".max.ru") || host.equals("oneme.ru") || host.endsWith(".oneme.ru"));
        } catch (IllegalArgumentException invalid) { return false; }
    }
}
