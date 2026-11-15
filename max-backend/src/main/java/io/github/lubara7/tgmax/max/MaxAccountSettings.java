package io.github.lubara7.tgmax.max;

import java.util.Map;
import java.util.Set;

/** Confirmed CONFIG user settings; excludes unrelated Telegram privacy keys. */
public final class MaxAccountSettings {
    private static final Set<String> ACCESS = Set.of("SEARCH_BY_PHONE", "INCOMING_CALL", "CHATS_INVITE", "PHONE_NUMBER_PRIVACY");
    private MaxAccountSettings() {}
    public static Map<String,Object> update(String key,Object value) {
        if(ACCESS.contains(key)) {
            if(!Set.of("ALL","CONTACTS","_NONE_").contains(value))throw new MaxException("MAX_PRIVACY_VALUE_INVALID");
        } else if(Set.of("HIDDEN","CONTENT_LEVEL_ACCESS").contains(key)) {
            if(!(value instanceof Boolean))throw new MaxException("MAX_PRIVACY_VALUE_INVALID");
        } else if("INACTIVE_TTL".equals(key)) {
            if(!Set.of("1M","3M","6M").contains(value))throw new MaxException("MAX_INACTIVE_TTL_INVALID");
        } else throw new MaxException("MAX_PRIVACY_KEY_UNSUPPORTED");
        return Maps.of("settings", Maps.of("user",Maps.of(key,value)));
    }
    public static int months(Object value) {
        if("1M".equals(value))return 1;if("3M".equals(value))return 3;if("6M".equals(value))return 6;
        throw new MaxException("MAX_INACTIVE_TTL_MISSING");
    }
}
