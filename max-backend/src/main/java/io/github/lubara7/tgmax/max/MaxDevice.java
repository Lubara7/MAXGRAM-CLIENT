package io.github.lubara7.tgmax.max;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class MaxDevice {
    public final String id;
    public volatile String language;
    public final String osVersion;
    public final String screen;
    public final String timezone;
    public final String deviceLocale;
    private volatile String userAgent;
    private Map<String,Object> fixedHandshake;
    public final MaxSessionProfile profile;
    private CompletableFuture<Void> ready = new CompletableFuture<>();

    public MaxDevice(String id, String language, String osVersion, String screen, String timezone, String userAgent) {
        this(id, language, osVersion, screen, timezone, userAgent,
                MaxSessionProfile.web(MaxSessionProfile.CURRENT_WEB_VERSION, null));
    }

    public MaxDevice(String id, String language, String osVersion, String screen, String timezone,
                     String userAgent, MaxSessionProfile profile) {
        this(id, language, osVersion, screen, timezone, userAgent, profile, language);
    }

    public MaxDevice(String id, String language, String osVersion, String screen, String timezone,
                     String userAgent, MaxSessionProfile profile, String deviceLocale) {
        if (!"WEB".equals(profile.deviceType)) throw new MaxException("MAX_TRANSPORT_PROFILE_MISMATCH");
        this.id = id;
        this.language = language;
        this.osVersion = osVersion;
        this.screen = screen;
        this.timezone = timezone;
        this.deviceLocale = deviceLocale;
        this.profile = profile;
        if (userAgent != null && !userAgent.isEmpty()) setUserAgent(userAgent);
    }

    public synchronized CompletableFuture<Void> ready() { return ready; }

    public synchronized void setUserAgent(String value) {
        if(fixedHandshake!=null && !java.util.Objects.equals(value,Maps.object(fixedHandshake.get("userAgent")).get("headerUserAgent"))) throw new MaxException("MAX_IMPORTED_PROFILE_LOCKED");
        profile.webHandshake(id, language, deviceLocale, osVersion, screen, timezone, value);
        userAgent = value;
        if (ready.isCompletedExceptionally()) ready = CompletableFuture.completedFuture(null);
        else ready.complete(null);
    }

    public synchronized void userAgentUnavailable() { ready.completeExceptionally(new MaxException("MAX_USER_AGENT_UNAVAILABLE")); }

    public synchronized Map<String, Object> handshake() {
        if(fixedHandshake!=null) return Maps.of("deviceId",fixedHandshake.get("deviceId"),"userAgent",new java.util.LinkedHashMap<>(Maps.object(fixedHandshake.get("userAgent"))));
        return profile.webHandshake(id, language, deviceLocale, osVersion, screen, timezone, userAgent);
    }
    public static MaxDevice fromWebHandshake(Map<String,Object> input) {
        Map<String,Object> init=MaxWebSessionImport.validateHandshake(input), agent=Maps.object(init.get("userAgent"));
        MaxDevice device=new MaxDevice(Maps.string(init.get("deviceId")),Maps.string(agent.get("locale")),Maps.string(agent.get("osVersion")),Maps.string(agent.get("screen")),
                Maps.string(agent.get("timezone")),Maps.string(agent.get("headerUserAgent")),MaxSessionProfile.web(Maps.string(agent.get("appVersion")),Maps.string(agent.get("deviceName"))),Maps.string(agent.get("deviceLocale")));
        device.fixedHandshake=init;return device;
    }
    public synchronized boolean hasFixedProfile(){return fixedHandshake!=null;}
    @Override public String toString() { return "MaxDevice([redacted])"; }
}
