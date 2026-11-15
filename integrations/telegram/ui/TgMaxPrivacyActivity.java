package io.github.lubara7.tgmax.telegram;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Map;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.PrivacyUsersActivity;

/** Only the six confirmed MAX privacy controls and supported security screens. */
public final class TgMaxPrivacyActivity extends BaseFragment {
    private LinearLayout list;
    private Context context;
    private boolean disposed,loading;
    public TgMaxPrivacyActivity(int account){setCurrentAccount(account);}
    @Override public View createView(Context context){
        this.context=context;actionBar.setTitle(LocaleController.getString(R.string.PrivacySettings));actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick(){@Override public void onItemClick(int id){if(id==-1)finishFragment();}});
        ScrollView scroll=new ScrollView(context);scroll.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        list=new LinearLayout(context);list.setOrientation(LinearLayout.VERTICAL);scroll.addView(list);fragmentView=scroll;load();return scroll;
    }
    private void load(){
        if(loading)return;loading=true;list.removeAllViews();status(LocaleController.getString(R.string.Loading));
        TgMaxRuntime.get(currentAccount).accountSettings().whenComplete((values,failure)->AndroidUtilities.runOnUIThread(()->{
            loading=false;if(disposed)return;list.removeAllViews();
            if(failure!=null){status(LocaleController.getString(R.string.ErrorOccurred));Button retry=new Button(context);retry.setText(LocaleController.getString(R.string.Retry));list.addView(retry);retry.setOnClickListener(v->load());}
            else {
                String[] keys={"SEARCH_BY_PHONE","INCOMING_CALL","CHATS_INVITE","CONTENT_LEVEL_ACCESS","HIDDEN","PHONE_NUMBER_PRIVACY"};
                int[] labels={R.string.TgMaxPrivacyFind,R.string.TgMaxPrivacyCall,R.string.TgMaxPrivacyInvite,R.string.TgMaxPrivacyContent,R.string.TgMaxPrivacyOnline,R.string.TgMaxPrivacyPhone};
                for(int i=0;i<keys.length;i++)if(values.containsKey(keys[i])){String key=keys[i];int label=labels[i];TextSettingsCell cell=row(LocaleController.getString(label),display(key,values.get(key)));cell.setOnClickListener(v->choose(key,label,values));}
            }
            TextSettingsCell blocked=row(LocaleController.getString(R.string.BlockedUsers),"");blocked.setOnClickListener(v->presentFragment(new PrivacyUsersActivity()));
            TextSettingsCell session=row(LocaleController.getString(R.string.TgMaxSessionProfile),"");session.setOnClickListener(v->presentFragment(new TgMaxSessionActivity(currentAccount)));
        }));
    }
    private String display(String key,Object value){
        if("HIDDEN".equals(key))return LocaleController.getString(Boolean.TRUE.equals(value)?R.string.LastSeenNobody:R.string.LastSeenEverybody);
        if("CONTENT_LEVEL_ACCESS".equals(key))return LocaleController.getString(Boolean.TRUE.equals(value)?R.string.TgMaxSafeContent:R.string.TgMaxAllContent);
        return LocaleController.getString("CONTACTS".equals(value)?R.string.LastSeenContacts:"_NONE_".equals(value)||"NOBODY".equals(value)?R.string.LastSeenNobody:R.string.LastSeenEverybody);
    }
    private void choose(String key,int label,Map<String,Object> current){
        boolean flag="HIDDEN".equals(key)||"CONTENT_LEVEL_ACCESS".equals(key);
        Object[] values=flag?new Object[]{false,true}:new Object[]{"ALL","CONTACTS","_NONE_"};
        CharSequence[] labels=new CharSequence[values.length];for(int i=0;i<labels.length;i++)labels[i]=display(key,values[i]);
        showDialog(new AlertDialog.Builder(getParentActivity()).setTitle(LocaleController.getString(label)).setItems(labels,(d,which)->{
            TgMaxRuntime.get(currentAccount).changeAccountSetting(key,values[which]).whenComplete((unused,error)->AndroidUtilities.runOnUIThread(()->{if(disposed)return;if(error!=null)TgMaxAccountControls.failure(this,error);load();}));
        }).create());
    }
    private TextSettingsCell row(String title,String value){TextSettingsCell cell=new TextSettingsCell(context);cell.setTextAndValue(title,value,true);list.addView(cell,LayoutHelper.createLinear(-1,56));return cell;}
    private void status(String value){TextView text=new TextView(context);text.setText(value);text.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));text.setPadding(AndroidUtilities.dp(20),AndroidUtilities.dp(16),AndroidUtilities.dp(20),AndroidUtilities.dp(16));list.addView(text);}
    @Override public void onFragmentDestroy(){disposed=true;super.onFragmentDestroy();}
}
