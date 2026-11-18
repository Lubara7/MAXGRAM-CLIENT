package io.github.lubara7.tgmax.telegram;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;

/** Only debug_ keys belong in this store; no session data is stored here. */
public final class TgMaxDebugState {
    private final Context context;
    private final SharedPreferences preferences;
    private final String loggingKey;

    public TgMaxDebugState(Context context, int account) {
        this.context = context.getApplicationContext();
        preferences = context.getSharedPreferences("maxgram_debug", Context.MODE_PRIVATE);
        loggingKey = "debug_protocol_logging_" + account;
    }

    public boolean unlocked() { return preferences.getBoolean("debug_menu_unlocked", false); }
    public boolean unlock() { return preferences.edit().putBoolean("debug_menu_unlocked", true).commit(); }
    public boolean allowed() { return (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0 || unlocked(); }
    public boolean logging() { return allowed() && preferences.getBoolean(loggingKey, false); }
    public void logging(boolean enabled) {
        if (enabled && !allowed()) throw new IllegalStateException("Debug menu is locked");
        preferences.edit().putBoolean(loggingKey, enabled).apply();
    }
}
