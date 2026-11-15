package io.github.lubara7.tgmax.telegram;

import io.github.lubara7.tgmax.max.MaxAccountSettings;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;

/** A server inactivity setting, never a local imitation of account deletion. */
public final class TgMaxAccountControls {
    private TgMaxAccountControls() {}
    public static void showInactivity(BaseFragment fragment,int account) {
        TgMaxRuntime runtime=TgMaxRuntime.get(account);
        runtime.accountSettings().whenComplete((settings,error)->AndroidUtilities.runOnUIThread(()->{
            if(fragment.getParentActivity()==null)return;
            if(error!=null){failure(fragment,error);return;}
            final int current;
            try{current=MaxAccountSettings.months(settings.get("INACTIVE_TTL"));}catch(RuntimeException e){failure(fragment,e);return;}
            int[] months={1,3,6};
            CharSequence[] labels={LocaleController.getString(R.string.TgMaxMonth1),LocaleController.getString(R.string.TgMaxMonth3),LocaleController.getString(R.string.TgMaxMonth6)};
            for(int i=0;i<months.length;i++)if(months[i]==current)labels[i]="✓ "+labels[i];
            fragment.showDialog(new AlertDialog.Builder(fragment.getParentActivity()).setTitle(LocaleController.getString(R.string.TgMaxDeleteInactive))
                    .setItems(labels,(d,which)->fragment.showDialog(new AlertDialog.Builder(fragment.getParentActivity())
                            .setTitle(LocaleController.getString(R.string.TgMaxDeleteInactive))
                            .setMessage(LocaleController.getString(R.string.TgMaxInactivityInfo)+"\n\n"+labels[which])
                            .setNegativeButton(LocaleController.getString(R.string.Cancel),null)
                            .setPositiveButton(LocaleController.getString(R.string.TgMaxApply),(dialog,button)->runtime.changeAccountSetting("INACTIVE_TTL",months[which]+"M")
                                    .whenComplete((unused,failed)->AndroidUtilities.runOnUIThread(()->{if(failed!=null)failure(fragment,failed);}))).create())).create());
        }));
    }
    static void failure(BaseFragment fragment,Throwable error){
        if(fragment.getParentActivity()!=null)fragment.showDialog(new AlertDialog.Builder(fragment.getParentActivity()).setTitle(LocaleController.getString(R.string.ErrorOccurred))
                .setMessage(TgMaxRuntime.errorText(error)).setPositiveButton(LocaleController.getString(R.string.OK),null).create());
    }
}
