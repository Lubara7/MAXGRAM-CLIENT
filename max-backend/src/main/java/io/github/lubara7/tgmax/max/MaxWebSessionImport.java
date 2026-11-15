package io.github.lubara7.tgmax.max;

import java.util.LinkedHashMap;
import java.util.Map;

/** Explicit own-session import. No browser storage or clipboard access. */
public final class MaxWebSessionImport {
    private final String token;
    private final Map<String, Object> init;
    public final Long viewerHint;
    private MaxWebSessionImport(String token, Long viewer, Map<String, Object> init) { this.token=token; viewerHint=viewer; this.init=init; }
    public static MaxWebSessionImport parse(Map<String,Object> auth, Map<String,Object> handshake) {
        if (!(auth.get("token") instanceof String)) throw invalid();
        String token = ((String)auth.get("token")).trim();
        if (token.isEmpty() || token.length()>65535 || token.chars().anyMatch(c -> c<=32 || c>126)) throw invalid();
        Long viewer = null;
        if (auth.get("viewerId")!=null) {
            String value=auth.get("viewerId").toString();
            if (!value.matches("[0-9]{1,19}")) throw invalid();
            try { viewer=Long.parseLong(value); } catch(NumberFormatException failure) {throw invalid();}
            if(viewer<=0) throw invalid();
        }
        return new MaxWebSessionImport(token,viewer,validateHandshake(handshake));
    }
    public static Map<String,Object> validateHandshake(Map<String,Object> frame) {
        if(frame.get("opcode")!=null && !"6".equals(frame.get("opcode").toString())) throw invalid();
        Map<String,Object> payload=frame.containsKey("payload")?Maps.object(frame.get("payload")):frame;
        String id=text(payload.get("deviceId"),128);
        Map<String,Object> source=Maps.object(payload.get("userAgent")), agent=new LinkedHashMap<>();
        for(String key:new String[]{"deviceType","locale","deviceLocale","osVersion","deviceName","headerUserAgent","appVersion","screen","timezone"}) agent.put(key,text(source.get(key),2048));
        if(!"WEB".equals(agent.get("deviceType"))) throw new MaxException("MAX_IMPORT_REQUIRES_WEB_PROFILE");
        if(source.containsKey("isPwa")) {if(!(source.get("isPwa") instanceof Boolean)) throw invalid(); agent.put("isPwa",source.get("isPwa"));}
        if(source.containsKey("pushDeviceType")) {if(!"WEBPUSH".equals(source.get("pushDeviceType"))) throw invalid(); agent.put("pushDeviceType","WEBPUSH");}
        MaxSessionProfile.web((String)agent.get("appVersion"),(String)agent.get("deviceName")).webHandshake(id,(String)agent.get("locale"),
                (String)agent.get("deviceLocale"),(String)agent.get("osVersion"),(String)agent.get("screen"),(String)agent.get("timezone"),(String)agent.get("headerUserAgent"));
        return Maps.of("deviceId",id,"userAgent",agent);
    }
    public String tokenForLogin(){return token;}
    public Map<String,Object> handshake(){return Maps.of("deviceId",init.get("deviceId"),"userAgent",new LinkedHashMap<>(Maps.object(init.get("userAgent"))));}
    public void requireViewer(long actual){if(actual<=0 || viewerHint!=null && viewerHint!=actual) throw new MaxException("MAX_IMPORT_ACCOUNT_MISMATCH");}
    private static String text(Object value,int maximum){
        if(!(value instanceof String)) throw invalid(); String text=(String)value;
        if(text.isEmpty()||text.length()>maximum||text.chars().anyMatch(c -> c<32||c==127))throw invalid();return text;
    }
    private static MaxException invalid(){return new MaxException("MAX_IMPORT_INVALID");}
    @Override public String toString(){return "MaxWebSessionImport([redacted])";}
}
