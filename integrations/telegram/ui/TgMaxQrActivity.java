package io.github.lubara7.tgmax.telegram;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import io.github.lubara7.tgmax.max.MaxQrLogin;
import java.util.EnumMap;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLObject;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

public final class TgMaxQrActivity extends BaseFragment {
    private final TgMaxRuntime runtime;
    private final BiConsumer<TLObject, Throwable> completion;
    private MaxQrLogin.Params params;
    private long deadline;
    private int generation;
    private boolean paused = true, disposed, inFlight;
    private boolean authorizing, authReady;
    private TLObject authResult;
    private Throwable authFailure;
    private CompletableFuture<?> operation;
    private ImageView code;
    private TextView status, refresh;
    private final Runnable poll = this::check;
    private final Runnable tick = this::updateTimer;

    public TgMaxQrActivity(int account, BiConsumer<TLObject, Throwable> completion) {
        setCurrentAccount(account);
        runtime = TgMaxRuntime.get(account);
        this.completion = completion;
    }

    @Override public View createView(Context context) {
        actionBar.setTitle(LocaleController.getString(R.string.TgMaxQrTitle));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });
        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setGravity(Gravity.CENTER_HORIZONTAL);
        list.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(24), AndroidUtilities.dp(24), AndroidUtilities.dp(24));
        list.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        fragmentView = list;
        TextView description = text(context, 16);
        description.setText(LocaleController.getString(R.string.TgMaxQrDescription));
        list.addView(description, LayoutHelper.createLinear(-1, -2));
        code = new ImageView(context);
        code.setContentDescription(LocaleController.getString(R.string.TgMaxQrTitle));
        list.addView(code, LayoutHelper.createLinear(220, 220, 0, 24, 0, 20));
        status = text(context, 14);
        list.addView(status, LayoutHelper.createLinear(-1, -2));
        refresh = text(context, 16);
        refresh.setText(LocaleController.getString(R.string.TgMaxQrRefresh));
        refresh.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        refresh.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(24),
                Theme.getColor(Theme.key_featuredStickers_addButton), Theme.getColor(Theme.key_featuredStickers_addButtonPressed)));
        list.addView(refresh, LayoutHelper.createLinear(-1, 48, 0, 24, 0, 0));
        refresh.setOnClickListener(v -> start());
        if (params != null && deadline > SystemClock.elapsedRealtime()) {
            try { code.setImageBitmap(bitmap(params.qrLink)); }
            catch (Exception invalid) { params = null; }
        }
        return fragmentView;
    }

    private void start() {
        if (paused || disposed || inFlight) return;
        cancel();
        params = null;
        code.setImageDrawable(null);
        status.setText(LocaleController.getString(R.string.TgMaxQrLoading));
        refresh.setEnabled(false);
        inFlight = true;
        final int epoch = generation;
        operation = runtime.requestQr().whenComplete((data, failure) -> AndroidUtilities.runOnUIThread(() -> {
            if (disposed || paused || epoch != generation) return;
            inFlight = false;
            if (failure != null) { fail(); return; }
            params = data;
            deadline = SystemClock.elapsedRealtime() + data.ttlMillis;
            try { code.setImageBitmap(bitmap(data.qrLink)); }
            catch (Exception invalid) { fail(); return; }
            updateTimer();
            check();
        }));
    }

    private void check() {
        if (disposed || paused || inFlight || params == null || SystemClock.elapsedRealtime() >= deadline) return;
        inFlight = true;
        final int epoch = generation;
        operation = runtime.checkQr(params.trackId).whenComplete((state, failure) -> AndroidUtilities.runOnUIThread(() -> {
            if (disposed || paused || epoch != generation) return;
            inFlight = false;
            if (failure != null) { fail(); return; }
            if (state.loginAvailable) { authorize(epoch); return; }
            deadline = SystemClock.elapsedRealtime() + state.ttlMillis;
            updateTimer();
            if (state.ttlMillis > 0) AndroidUtilities.runOnUIThread(poll, params.pollingMillis);
        }));
    }

    private void authorize(int epoch) {
        inFlight = true; authorizing = true;
        operation = runtime.finishCall(params.trackId).whenComplete((result, failure) -> AndroidUtilities.runOnUIThread(() -> {
            if (disposed || epoch != generation) return;
            inFlight = false; authorizing = false; authReady = true;
            authResult = result; authFailure = failure;
            if (!paused) deliverAuth();
        }));
    }

    private void deliverAuth() {
        authReady = false;
        if (authFailure != null && !runtime.needsPassword()) { fail(); return; }
        finishFragment();
        completion.accept(authResult, authFailure);
    }

    private void updateTimer() {
        AndroidUtilities.cancelRunOnUIThread(tick);
        if (paused || disposed || params == null) return;
        long left = Math.max(0, deadline - SystemClock.elapsedRealtime());
        refresh.setEnabled(left == 0 && !inFlight);
        if (left == 0) {
            code.setImageDrawable(null);
            status.setText(LocaleController.getString(R.string.TgMaxQrExpired));
            AndroidUtilities.cancelRunOnUIThread(poll);
        } else {
            long seconds = (left + 999) / 1000;
            status.setText(String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60));
            AndroidUtilities.runOnUIThread(tick, 1000);
        }
    }

    private void fail() {
        cancel(); params = null;
        code.setImageDrawable(null);
        status.setText(LocaleController.getString(R.string.ErrorOccurred));
        refresh.setEnabled(true);
    }

    private void cancel() {
        generation++;
        if (operation != null) operation.cancel(false);
        operation = null; inFlight = false;
        AndroidUtilities.cancelRunOnUIThread(poll);
        AndroidUtilities.cancelRunOnUIThread(tick);
    }

    @Override public void onResume() {
        super.onResume(); paused = false;
        if (authReady) deliverAuth();
        else if (!authorizing) { if (params == null) start(); else { updateTimer(); check(); } }
    }
    @Override public void onPause() {
        paused = true;
        if (authorizing) {
            AndroidUtilities.cancelRunOnUIThread(poll);
            AndroidUtilities.cancelRunOnUIThread(tick);
        } else cancel();
        super.onPause();
    }
    @Override public void onFragmentDestroy() { disposed = true; cancel(); super.onFragmentDestroy(); }

    private static TextView text(Context context, int size) {
        TextView view = new TextView(context);
        view.setTextSize(size); view.setGravity(Gravity.CENTER);
        view.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        return view;
    }

    private static Bitmap bitmap(String link) throws Exception {
        int side = AndroidUtilities.dp(220);
        EnumMap<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 2);
        BitMatrix matrix = new QRCodeWriter().encode(link, BarcodeFormat.QR_CODE, side, side, hints);
        int[] pixels = new int[side * side];
        for (int y = 0; y < side; y++) for (int x = 0; x < side; x++) pixels[y * side + x] = matrix.get(x, y) ? 0xff000000 : 0xffffffff;
        return Bitmap.createBitmap(pixels, side, side, Bitmap.Config.ARGB_8888);
    }
}
