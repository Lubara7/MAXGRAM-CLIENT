package io.github.lubara7.tgmax.max;
import org.junit.Test;
import static org.junit.Assert.*;
public class MaxAccountSettingsTest {
    @Test public void inactivityIsACalendarPeriodInUserConfig(){
        for(int month:new int[]{1,3,6}){
            assertEquals(month,MaxAccountSettings.months(month+"M"));
            assertEquals(month+"M",Maps.object(Maps.object(MaxAccountSettings.update("INACTIVE_TTL",month+"M").get("settings")).get("user")).get("INACTIVE_TTL"));
        }
    }
    @Test public void onlyConfirmedPrivacyKeysAndValuesAreSent(){
        assertEquals(Boolean.TRUE,Maps.object(Maps.object(MaxAccountSettings.update("HIDDEN",true).get("settings")).get("user")).get("HIDDEN"));
        try{MaxAccountSettings.update("PREMIUM",true);fail();}catch(MaxException expected){assertEquals("MAX_PRIVACY_KEY_UNSUPPORTED",expected.code);}
        try{MaxAccountSettings.update("INACTIVE_TTL",90);fail();}catch(MaxException expected){assertEquals("MAX_INACTIVE_TTL_INVALID",expected.code);}
    }
    @Test public void absentServerValueIsNotPretendedToBeTheDefault(){
        try{MaxAccountSettings.months(null);fail();}catch(MaxException expected){assertEquals("MAX_INACTIVE_TTL_MISSING",expected.code);}
    }
}
