package io.github.lubara7.tgmax.telegram;

import android.app.Activity;
import java.lang.ref.WeakReference;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;

public final class TgMaxDisclaimer {
    private static WeakReference<AlertDialog> shown = new WeakReference<>(null);
    private static WeakReference<Activity> owner = new WeakReference<>(null);
    private TgMaxDisclaimer() {}
    public static void showIfNeeded(Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed() || TgMaxConsent.accepted(activity)) return;
        AlertDialog old = shown.get();
        if (old != null && old.isShowing()) { if (owner.get() == activity) return; old.dismiss(); }
        TgMaxConsent.begin(activity);
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(LocaleController.getString(R.string.TgMaxUnofficialTitle))
                .setMessage(LocaleController.getString(R.string.TgMaxUnofficialWarning))
                .setPositiveButton(LocaleController.getString(R.string.TgMaxRiskAccept), (d, which) -> {
                    if (TgMaxConsent.accept(activity)) TgMaxRuntime.resumeAfterConsent(); else activity.finish();
                }).setNegativeButton(LocaleController.getString(R.string.TgMaxRiskDecline), (d, which) -> {
                    TgMaxConsent.decline(activity); activity.finish();
                }).create();
        dialog.setCancelable(false); dialog.setCanceledOnTouchOutside(false);
        dialog.setOnDismissListener(d -> { if (shown.get() == dialog) shown.clear(); });
        shown = new WeakReference<>(dialog); owner = new WeakReference<>(activity); dialog.show();
    }
}
