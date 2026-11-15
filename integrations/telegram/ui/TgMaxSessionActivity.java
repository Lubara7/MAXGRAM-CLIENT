package io.github.lubara7.tgmax.telegram;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;

public final class TgMaxSessionActivity extends BaseFragment {
    private String selected;
    public TgMaxSessionActivity(int account){setCurrentAccount(account);}
    @Override public View createView(Context context){
        actionBar.setTitle(LocaleController.getString(R.string.TgMaxSessionProfile));actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick(){@Override public void onItemClick(int id){if(id==-1)finishFragment();}});
        SharedPreferences p=context.getSharedPreferences("max_device_"+currentAccount,0);
        String recommended=TgMaxRuntime.recommendedWebVersion(context);
        selected=TgMaxRuntime.get(currentAccount).sessionVersion();
        ScrollView scroll=new ScrollView(context);scroll.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        LinearLayout list=new LinearLayout(context);list.setOrientation(LinearLayout.VERTICAL);scroll.addView(list);
        TextView info=new TextView(context);info.setText(LocaleController.getString(R.string.TgMaxSessionInfo));info.setTextSize(14);
        info.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));info.setPadding(AndroidUtilities.dp(20),AndroidUtilities.dp(16),AndroidUtilities.dp(20),AndroidUtilities.dp(16));list.addView(info,LayoutHelper.createLinear(-1,-2));
        TextSettingsCell version=new TextSettingsCell(context);version.setTextAndValue(LocaleController.getString(R.string.TgMaxSessionVersion),selected,true);list.addView(version);
        version.setOnClickListener(v->{
            CharSequence[] values=selected.equals(recommended)?new CharSequence[]{recommended}:new CharSequence[]{recommended,selected};
            showDialog(new AlertDialog.Builder(getParentActivity()).setTitle(LocaleController.getString(R.string.TgMaxSessionVersion)).setItems(values,(dialog,which)->{selected=values[which].toString();version.setTextAndValue(LocaleController.getString(R.string.TgMaxSessionVersion),selected,true);}).create());
        });
        TextCheckCell auto=new TextCheckCell(context);auto.setTextAndCheck(LocaleController.getString(R.string.TgMaxSessionAuto),p.getBoolean("auto_profile",false),true);list.addView(auto);
        auto.setOnClickListener(v->{boolean enabled=!p.getBoolean("auto_profile",false);p.edit().putBoolean("auto_profile",enabled).apply();auto.setChecked(enabled);});
        Button reset=new Button(context);reset.setText("R.S.V");list.addView(reset,LayoutHelper.createLinear(-1,56));reset.setOnClickListener(v->{selected=recommended;version.setTextAndValue(LocaleController.getString(R.string.TgMaxSessionVersion),selected,true);});
        Button apply=new Button(context);apply.setText(LocaleController.getString(R.string.TgMaxApply));list.addView(apply,LayoutHelper.createLinear(-1,56));
        apply.setOnClickListener(v->showDialog(new AlertDialog.Builder(getParentActivity()).setTitle(LocaleController.getString(R.string.TgMaxSessionProfile))
                .setMessage(LocaleController.getString(R.string.TgMaxSessionRelogin)).setNegativeButton(LocaleController.getString(R.string.Cancel),null)
                .setPositiveButton(LocaleController.getString(R.string.TgMaxApply),(dialog,which)->{
                    if(!p.edit().putString("app_version",selected).putBoolean("legacy_profile",false).commit()){TgMaxAccountControls.failure(this,new IllegalStateException());return;}
                    MessagesController.getInstance(currentAccount).performLogout(2);
                }).create()));
        return fragmentView=scroll;
    }
}
