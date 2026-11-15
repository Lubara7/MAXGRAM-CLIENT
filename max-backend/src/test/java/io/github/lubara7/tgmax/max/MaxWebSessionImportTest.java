package io.github.lubara7.tgmax.max;

import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class MaxWebSessionImportTest {
    private Map<String,Object> init(){return Maps.of("deviceId","original-browser-id","userAgent",Maps.of("deviceType","WEB","locale","ru","deviceLocale","en","osVersion","Linux","deviceName","Chrome","headerUserAgent","original-browser","appVersion","26.11.6","screen","800x360 3.0x","timezone","UTC","isPwa",true));}
    @Test public void retainsOriginalIdentityAndBindsViewer(){
        MaxWebSessionImport s=MaxWebSessionImport.parse(Maps.of("token","synthetic-session","viewerId","123"),init());
        s.requireViewer(123); assertEquals("original-browser-id",s.handshake().get("deviceId"));
        assertTrue((Boolean)Maps.object(s.handshake().get("userAgent")).get("isPwa"));
        try{s.requireViewer(124);fail();}catch(MaxException expected){assertEquals("MAX_IMPORT_ACCOUNT_MISMATCH",expected.code);}
        assertFalse(s.toString().contains("synthetic-session"));
    }
    @Test public void rejectsMobileProfileAndMalformedCredential(){
        Map<String,Object> raw=init();Maps.object(raw.get("userAgent")).put("deviceType","ANDROID");
        try{MaxWebSessionImport.parse(Maps.of("token","test"),raw);fail();}catch(MaxException expected){assertEquals("MAX_IMPORT_REQUIRES_WEB_PROFILE",expected.code);}
        try{MaxWebSessionImport.parse(Maps.of("token","secret\nheader"),init());fail();}catch(MaxException expected){assertEquals("MAX_IMPORT_INVALID",expected.code);}
    }
    @Test public void copiedMapCannotMutateOrRedirectAnImportedSession(){
        Map<String,Object> raw=init(); raw.put("endpoint","https://attacker.invalid");
        MaxWebSessionImport s=MaxWebSessionImport.parse(Maps.of("token","test"),raw);
        Maps.object(raw.get("userAgent")).put("headerUserAgent","changed");
        Map<String,Object> copy=s.handshake();Maps.object(copy.get("userAgent")).put("headerUserAgent","changed-again");
        assertEquals("original-browser",Maps.object(s.handshake().get("userAgent")).get("headerUserAgent"));
        assertFalse(s.handshake().containsKey("endpoint"));
    }
    @Test public void aRestoredDeviceKeepsTheOriginalBrowserProfile(){
        MaxDevice device=MaxDevice.fromWebHandshake(init());assertTrue(device.hasFixedProfile());
        assertEquals("original-browser",Maps.object(device.handshake().get("userAgent")).get("headerUserAgent"));
        try{device.setUserAgent("Android-WebView");fail();}catch(MaxException expected){assertEquals("MAX_IMPORTED_PROFILE_LOCKED",expected.code);}
    }
}
