package io.github.lubara7.tgmax.telegram;

import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
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

/** Manual owner-provided credentials; no clipboard scraping or saved input state. */
public final class TgMaxTokenActivity extends BaseFragment {
    private final BiConsumer<TLObject,Throwable> completion;
    private EditText credential,profile;
    private Button submit;
    private CompletableFuture<TLObject> operation;
    private boolean disposed,wasSecure;
    private Window window;
    public TgMaxTokenActivity(int account,BiConsumer<TLObject,Throwable> completion){setCurrentAccount(account);this.completion=completion;}
    @Override public View createView(Context context){
        actionBar.setTitle(LocaleController.getString(R.string.TgMaxTokenLogin));actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick(){@Override public void onItemClick(int id){if(id==-1)finishFragment();}});
        ScrollView scroll=new ScrollView(context);scroll.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        LinearLayout list=new LinearLayout(context);list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(AndroidUtilities.dp(24),AndroidUtilities.dp(20),AndroidUtilities.dp(24),AndroidUtilities.dp(24));scroll.addView(list);
        TextView info=new TextView(context);info.setText(LocaleController.getString(R.string.TgMaxTokenInstructions));info.setTextSize(15);info.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));list.addView(info,LayoutHelper.createLinear(-1,-2));
        credential=input(context,list,R.string.TgMaxTokenCredential,true);profile=input(context,list,R.string.TgMaxTokenProfile,false);
        submit=new Button(context);submit.setText(LocaleController.getString(R.string.TgMaxTokenLogin));list.addView(submit,LayoutHelper.createLinear(-1,56));submit.setOnClickListener(v->login());
        if(getParentActivity()!=null){window=getParentActivity().getWindow();wasSecure=(window.getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0;window.addFlags(WindowManager.LayoutParams.FLAG_SECURE);}
        return fragmentView=scroll;
    }
    private static EditText input(Context context,LinearLayout list,int hint,boolean secret){
        EditText view=new EditText(context);view.setHint(LocaleController.getString(hint));view.setTextSize(14);view.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));view.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        view.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS|(secret?InputType.TYPE_TEXT_VARIATION_PASSWORD:InputType.TYPE_TEXT_FLAG_MULTI_LINE));
        view.setSaveEnabled(false);view.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);view.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
        view.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(secret?131072:32768)});view.setMinLines(secret?2:5);list.addView(view,LayoutHelper.createLinear(-1,-2));return view;
    }
    private void login(){
        if(operation!=null)return;submit.setEnabled(false);credential.setEnabled(false);profile.setEnabled(false);AndroidUtilities.hideKeyboard(credential);
        operation=TgMaxRuntime.get(currentAccount).importWebSession(credential.getText().toString(),profile.getText().toString());
        operation.whenComplete((result,error)->AndroidUtilities.runOnUIThread(()->{
            if(disposed)return;operation=null;
            if(error!=null){submit.setEnabled(true);credential.setEnabled(true);profile.setEnabled(true);TgMaxAccountControls.failure(this,error);}
            else{credential.setText("");profile.setText("");finishFragment();completion.accept(result,null);}
        }));
    }
    @Override public void onFragmentDestroy(){
        disposed=true;if(operation!=null)operation.cancel(false);if(credential!=null)credential.setText("");if(profile!=null)profile.setText("");
        if(window!=null&&!wasSecure)window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE);window=null;super.onFragmentDestroy();
    }
}
