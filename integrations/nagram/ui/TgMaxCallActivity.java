package io.github.lubara7.tgmax.nagram;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import io.github.lubara7.tgmax.max.MaxCallOut;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import org.telegram.PhoneFormat.PhoneFormat;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLObject;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

public final class TgMaxCallActivity extends BaseFragment {
    private final TgMaxRuntime runtime;
    private final MaxCallOut.Params params;
    private final String phone;
    private final BiConsumer<TLObject, Throwable> completion;
    private TextView timer, button;
    private String callNumber = "";
    private final long deadline;
    private boolean stopped, disposed, inFlight;
    private CompletableFuture<?> operation;
    private final Runnable poll = this::check;
    private final Runnable tick = this::updateTimer;

    public TgMaxCallActivity(int account, String phone, MaxCallOut.Params params, BiConsumer<TLObject, Throwable> completion) {
        super();
        this.runtime = TgMaxRuntime.get(account);
        this.phone = phone;
        this.params = params;
        this.completion = completion;
        this.deadline = SystemClock.elapsedRealtime() + params.ttlMillis;
    }

    @Override public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        actionBar.setItemsColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), false);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });
        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        fragmentView = root;
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(content, LayoutHelper.createFrame(-1, -2, Gravity.TOP, 24, 44, 24, 0));
        TextView title = text(context, 24, Theme.key_windowBackgroundWhiteBlackText);
        title.setText(LocaleController.formatString(R.string.TgMaxCallTitle, PhoneFormat.getInstance().format(phone)));
        content.addView(title, LayoutHelper.createLinear(-1, -2));
        TextView description = text(context, 16, Theme.key_windowBackgroundWhiteGrayText);
        description.setText(LocaleController.getString(R.string.TgMaxCallDescription));
        content.addView(description, LayoutHelper.createLinear(-1, -2, 0, 18, 0, 0));
        LinearLayout bottom = new LinearLayout(context);
        bottom.setOrientation(LinearLayout.VERTICAL);
        root.addView(bottom, LayoutHelper.createFrame(-1, -2, Gravity.BOTTOM, 24, 0, 24, 24));
        timer = text(context, 14, Theme.key_windowBackgroundWhiteGrayText);
        bottom.addView(timer, LayoutHelper.createLinear(-1, -2, 0, 0, 0, 18));
        button = text(context, 16, Theme.key_featuredStickers_buttonText);
        button.setGravity(Gravity.CENTER);
        button.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(28),
                Theme.getColor(Theme.key_featuredStickers_addButton), Theme.getColor(Theme.key_featuredStickers_addButtonPressed)));
        button.setText(LocaleController.getString(R.string.TgMaxCallWaiting));
        button.setEnabled(false);
        bottom.addView(button, LayoutHelper.createLinear(-1, 56));
        button.setOnClickListener(v -> {
            if (stopped) { refresh(); return; }
            if (callNumber.isEmpty() || SystemClock.elapsedRealtime() >= deadline) return;
            try {
                getParentActivity().startActivity(new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", callNumber, null)));
            } catch (android.content.ActivityNotFoundException error) {
                timer.setText(LocaleController.getString(R.string.TgMaxCallDialerMissing));
            }
        });
        AndroidUtilities.cancelRunOnUIThread(tick);
        AndroidUtilities.cancelRunOnUIThread(poll);
        updateTimer();
        check();
        return root;
    }

    private TextView text(Context context, int size, int key) {
        TextView view = new TextView(context);
        view.setTextSize(size);
        view.setGravity(Gravity.CENTER);
        view.setTextColor(Theme.getColor(key));
        view.setLineSpacing(AndroidUtilities.dp(3), 1f);
        return view;
    }

    private void updateTimer() {
        if (disposed || stopped) return;
        long remaining = Math.max(0, deadline - SystemClock.elapsedRealtime());
        if (remaining == 0) { expire(); return; }
        long seconds = (remaining + 999) / 1000;
        timer.setText(LocaleController.formatString(R.string.TgMaxCallTimer,
                String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60)));
        AndroidUtilities.runOnUIThread(tick, Math.min(1000, remaining));
    }

    private void check() {
        if (disposed || stopped || inFlight) return;
        if (SystemClock.elapsedRealtime() >= deadline) { expire(); return; }
        inFlight = true;
        operation = runtime.checkCall(params.trackId).whenComplete((data, failure) -> AndroidUtilities.runOnUIThread(() -> {
            inFlight = false;
            if (disposed || stopped) return;
            if (SystemClock.elapsedRealtime() >= deadline) { expire(); return; }
            if (failure != null) {
                String code = TgMaxRuntime.errorText(failure);
                if (!code.equals("MAX_TIMEOUT") && !code.startsWith("MAX_NETWORK") && !code.equals("MAX_NOT_CONNECTED")) {
                    fail(failure); return;
                }
            } else if (data.status == 2) {
                stopped = true;
                AndroidUtilities.cancelRunOnUIThread(tick);
                button.setEnabled(false);
                button.setText(LocaleController.getString(R.string.TgMaxCallSigningIn));
                operation = runtime.finishCall(params.trackId).whenComplete((auth, error) -> AndroidUtilities.runOnUIThread(() -> {
                    if (!disposed) { finishFragment(false); completion.accept(auth, error); }
                }));
                return;
            } else if (data.status == 3) { expire(); return; }
            else if (data.status == 1) {
                callNumber = data.callNumber;
                button.setText(LocaleController.formatString(R.string.TgMaxCallButton, PhoneFormat.getInstance().format(callNumber)));
                button.setEnabled(true);
            }
            AndroidUtilities.runOnUIThread(poll, Math.min(params.pollingIntervalMillis, Math.max(1, deadline - SystemClock.elapsedRealtime())));
        }));
    }

    private void expire() {
        stopped = true;
        AndroidUtilities.cancelRunOnUIThread(poll);
        AndroidUtilities.cancelRunOnUIThread(tick);
        timer.setText(LocaleController.getString(R.string.TgMaxCallExpired));
        button.setText(LocaleController.getString(R.string.TgMaxCallRefresh));
        button.setEnabled(true);
    }

    private void refresh() {
        button.setEnabled(false);
        operation = runtime.refreshPhoneCode().whenComplete((code, failure) -> AndroidUtilities.runOnUIThread(() -> {
            if (!disposed) { finishFragment(false); completion.accept(code, failure); }
        }));
    }

    private void fail(Throwable failure) {
        stopped = true;
        AndroidUtilities.cancelRunOnUIThread(tick);
        finishFragment(false);
        completion.accept(null, failure);
    }

    @Override public void onFragmentDestroy() {
        disposed = true;
        AndroidUtilities.cancelRunOnUIThread(poll);
        AndroidUtilities.cancelRunOnUIThread(tick);
        if (operation != null) operation.cancel(false);
        super.onFragmentDestroy();
    }
}
