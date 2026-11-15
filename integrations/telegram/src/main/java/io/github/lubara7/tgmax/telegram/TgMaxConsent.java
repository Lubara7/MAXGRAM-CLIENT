package io.github.lubara7.tgmax.telegram;

import android.content.Context;
import io.github.lubara7.tgmax.max.MaxException;
import java.util.concurrent.CompletableFuture;

/** Required acknowledgement before opening any MAX socket. */
public final class TgMaxConsent {
    private static final int VERSION = 2;
    private static CompletableFuture<Void> permission;
    private TgMaxConsent() {}
    public static boolean accepted(Context context) {
        return context.getSharedPreferences("maxgram_consent", Context.MODE_PRIVATE).getInt("accepted_version", 0) >= VERSION;
    }
    public static synchronized CompletableFuture<Void> permission(Context context) {
        if (permission == null) permission = accepted(context) ? CompletableFuture.completedFuture(null) : new CompletableFuture<>();
        return permission;
    }
    public static synchronized void begin(Context context) {
        if (accepted(context)) permission = CompletableFuture.completedFuture(null);
        else if (permission == null || permission.isCompletedExceptionally()) permission = new CompletableFuture<>();
    }
    public static synchronized boolean accept(Context context) {
        if (!context.getSharedPreferences("maxgram_consent", Context.MODE_PRIVATE).edit().putInt("accepted_version", VERSION).commit()) return false;
        if (permission == null || permission.isCompletedExceptionally()) permission = CompletableFuture.completedFuture(null);
        else permission.complete(null);
        return true;
    }
    public static synchronized void decline(Context context) {
        if (!accepted(context)) permission(context).completeExceptionally(new MaxException("MAX_CONSENT_DECLINED"));
    }
}
