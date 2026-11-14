package io.github.lubara7.tgmax.telegram;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import io.github.lubara7.tgmax.max.MaxPrivacyPolicy;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.LayoutHelper;

public final class TgMaxSettingsActivity extends BaseFragment {
    private final TgMaxRuntime runtime;
    private TextCheckCell ghost, unread, typing;

    public TgMaxSettingsActivity(int account) { setCurrentAccount(account); runtime = TgMaxRuntime.get(account); }

    @Override public View createView(Context context) {
        actionBar.setTitle(LocaleController.getString(R.string.TgMaxLocalSettings));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });
        ScrollView scroll = new ScrollView(context);
        scroll.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        fragmentView = scroll;
        ghost = row(context, list, R.string.TgMaxGhost, () -> {
            MaxPrivacyPolicy p = runtime.privacy();
            runtime.privacy(new MaxPrivacyPolicy(!p.ghost, p.unread, p.noTyping));
            refresh();
        });
        unread = row(context, list, R.string.TgMaxUnread, () -> {
            MaxPrivacyPolicy p = runtime.privacy();
            runtime.privacy(new MaxPrivacyPolicy(p.ghost, !p.unread, p.noTyping));
            refresh();
        });
        typing = row(context, list, R.string.TgMaxNoTyping, () -> {
            MaxPrivacyPolicy p = runtime.privacy();
            runtime.privacy(new MaxPrivacyPolicy(p.ghost, p.unread, !p.noTyping));
            refresh();
        });
        info(context, list, R.string.TgMaxGhostInfo);
        animation(context, list, R.string.TgMaxAnimatedStickers, LiteMode.FLAGS_ANIMATED_STICKERS);
        animation(context, list, R.string.TgMaxAnimatedEmoji, LiteMode.FLAGS_ANIMATED_EMOJI);
        info(context, list, R.string.TgMaxAnimationInfo);
        refresh();
        return fragmentView;
    }

    private void refresh() {
        MaxPrivacyPolicy p = runtime.privacy();
        ghost.setChecked(p.ghost);
        unread.setChecked(!p.sendsRead());
        typing.setChecked(!p.sendsTyping());
        unread.setEnabled(!p.ghost); typing.setEnabled(!p.ghost);
        unread.setAlpha(p.ghost ? 0.5f : 1f); typing.setAlpha(p.ghost ? 0.5f : 1f);
    }

    private static TextCheckCell row(Context context, LinearLayout list, int label, Runnable action) {
        TextCheckCell cell = new TextCheckCell(context);
        cell.setTextAndCheck(LocaleController.getString(label), false, true);
        list.addView(cell, LayoutHelper.createLinear(-1, 56));
        cell.setOnClickListener(v -> action.run());
        return cell;
    }

    private static void animation(Context context, LinearLayout list, int label, int flag) {
        TextCheckCell cell = row(context, list, label, () -> {});
        cell.setChecked(LiteMode.isEnabledSetting(flag));
        cell.setOnClickListener(v -> {
            LiteMode.toggleFlag(flag, !LiteMode.isEnabledSetting(flag));
            cell.setChecked(LiteMode.isEnabledSetting(flag));
        });
    }

    private static void info(Context context, LinearLayout list, int label) {
        TextView view = new TextView(context);
        view.setText(LocaleController.getString(label));
        view.setTextSize(14);
        view.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        view.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(12), AndroidUtilities.dp(20), AndroidUtilities.dp(20));
        list.addView(view, LayoutHelper.createLinear(-1, -2));
    }
}
