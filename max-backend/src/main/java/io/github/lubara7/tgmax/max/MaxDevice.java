package io.github.lubara7.tgmax.max;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MaxDevice {
    public final String id;
    public volatile String language;
    public final String osVersion;
    public final String screen;
    public final String timezone;
    public volatile String userAgent;

    public MaxDevice(String id, String language, String osVersion, String screen, String timezone, String userAgent) {
        this.id = id;
        this.language = language;
        this.osVersion = osVersion;
        this.screen = screen;
        this.timezone = timezone;
        this.userAgent = userAgent;
    }

    public Map<String, Object> handshake() {
        return Maps.of("deviceId", id, "userAgent", Maps.of(
                "deviceType", "WEB", "locale", language, "deviceLocale", language,
                "osVersion", osVersion, "deviceName", "MAXGRAM CLIENT", "headerUserAgent", userAgent,
                "appVersion", "26.8.8", "screen", screen, "timezone", timezone, "isPwa", false));
    }

    @Override public String toString() { return "MaxDevice([redacted])"; }
}
