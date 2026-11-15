package io.github.lubara7.tgmax.telegram;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Local display names only. No address-book upload or phonebook writes. */
final class MaxPhonebookNames {
    private final Context context;
    private volatile Map<String,String> names=new HashMap<>();
    MaxPhonebookNames(Context context){this.context=context;}
    void refresh(String country){
        Map<String,String> next=new HashMap<>();
        if(context.checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED){names=next;return;}
        if(country==null||!country.matches("[A-Za-z]{2}"))country=Locale.getDefault().getCountry();
        String region=country.toUpperCase(Locale.ROOT);
        try(Cursor cursor=context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER,ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME},null,null,ContactsContract.CommonDataKinds.Phone._ID+" ASC")){
            if(cursor!=null)while(cursor.moveToNext()){
                String number=cursor.getString(1),display=cursor.getString(2);
                if(number==null||number.isEmpty())number=PhoneNumberUtils.formatNumberToE164(cursor.getString(0),region);
                if(number!=null&&number.startsWith("+")&&display!=null&&!display.trim().isEmpty())next.putIfAbsent(number.replaceAll("[^0-9]",""),display.trim());
            }
        }catch(SecurityException ignored){}catch(RuntimeException ignored){}
        names=next;
    }
    String find(String fullPhone){return names.get(fullPhone);}
}
