package io.github.lubara7.tgmax.telegram;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.nio.charset.StandardCharsets;
import java.util.WeakHashMap;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;

public final class TgMaxDebugActivity extends BaseFragment {
    private static final int EXPORT_LOG = 7704;
    private static final WeakHashMap<View, Taps> taps = new WeakHashMap<>();
    private TextView log;
    private TgMaxRuntime runtime;

    public TgMaxDebugActivity(int account) { setCurrentAccount(account); }

    public static void versionTap(BaseFragment fragment, View view, int account) {
        TgMaxDebugState state = new TgMaxDebugState(ApplicationLoader.applicationContext, account);
        if (!state.unlocked()) {
            long now = SystemClock.elapsedRealtime();
            Taps progress = taps.get(view);
            if (progress == null || now - progress.last > 10_000) {
                progress = new Taps(); taps.put(view, progress);
            }
            progress.last = now;
            if (++progress.count < 7) return;
            taps.remove(view);
            if (!state.unlock()) return;
        }
        fragment.presentFragment(new TgMaxDebugActivity(account));
    }

    @Override public boolean onFragmentCreate() {
        return (BuildConfig.DEBUG || new TgMaxDebugState(ApplicationLoader.applicationContext, currentAccount).unlocked())
                && super.onFragmentCreate();
    }

    @Override public View createView(Context context) {
        runtime = TgMaxRuntime.get(currentAccount);
        actionBar.setTitle(LocaleController.getString(R.string.TgMaxDebugTitle));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });
        ScrollView scroll = new ScrollView(context);
        scroll.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        LinearLayout list = new LinearLayout(context); list.setOrientation(LinearLayout.VERTICAL); scroll.addView(list);
        TextView info = new TextView(context);
        info.setText(LocaleController.getString(R.string.TgMaxDebugInfo)); info.setTextSize(14);
        info.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        info.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(16), AndroidUtilities.dp(20), AndroidUtilities.dp(16));
        list.addView(info, LayoutHelper.createLinear(-1, -2));
        TextCheckCell enabled = new TextCheckCell(context);
        enabled.setTextAndCheck(LocaleController.getString(R.string.TgMaxDebugRecord), runtime.protocolLogging(), true);
        list.addView(enabled, LayoutHelper.createLinear(-1, 56));
        enabled.setOnClickListener(v -> {
            runtime.protocolLogging(!runtime.protocolLogging()); enabled.setChecked(runtime.protocolLogging()); refresh();
        });
        row(context, list, R.string.TgMaxDebugRefresh, this::refresh);
        row(context, list, R.string.TgMaxDebugExport, () -> {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("text/plain").putExtra(Intent.EXTRA_TITLE, "maxgram-protocol.txt");
            try { startActivityForResult(intent, EXPORT_LOG); }
            catch (RuntimeException error) { TgMaxAccountControls.failure(this, error); }
        });
        row(context, list, R.string.TgMaxDebugClear, () -> { runtime.clearProtocolLog(); refresh(); });
        log = new TextView(context); log.setTextSize(12); log.setTypeface(android.graphics.Typeface.MONOSPACE);
        log.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText)); log.setTextIsSelectable(true);
        log.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(16), AndroidUtilities.dp(20), AndroidUtilities.dp(24));
        list.addView(log, LayoutHelper.createLinear(-1, -2)); refresh();
        return fragmentView = scroll;
    }

    private void refresh() { if (log != null) log.setText(runtime.protocolLog()); }

    private static void row(Context context, LinearLayout list, int label, Runnable action) {
        TextSettingsCell row = new TextSettingsCell(context); row.setText(LocaleController.getString(label), true);
        list.addView(row, LayoutHelper.createLinear(-1, 56)); row.setOnClickListener(v -> action.run());
    }

    @Override public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (requestCode != EXPORT_LOG) { super.onActivityResultFragment(requestCode, resultCode, data); return; }
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) return;
        android.net.Uri destination = data.getData();
        byte[] contents = runtime.protocolLog().getBytes(StandardCharsets.UTF_8);
        Context context = ApplicationLoader.applicationContext;
        Utilities.globalQueue.postRunnable(() -> {
            try (java.io.OutputStream stream = context.getContentResolver().openOutputStream(destination, "wt")) {
                if (stream == null) throw new java.io.IOException("No document stream");
                stream.write(contents);
            } catch (Exception error) {
                AndroidUtilities.runOnUIThread(() -> { if (getParentActivity() != null) TgMaxAccountControls.failure(this, error); });
                return;
            }
            AndroidUtilities.runOnUIThread(() -> {
                if (getParentActivity() != null) Toast.makeText(getParentActivity(), LocaleController.getString(R.string.TgMaxDebugSaved), Toast.LENGTH_SHORT).show();
            });
        });
    }

    private static final class Taps { int count; long last; }
}
