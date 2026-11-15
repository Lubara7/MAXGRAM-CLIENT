package io.github.lubara7.tgmax.telegram;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import io.github.lubara7.tgmax.max.Maps;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.json.JSONArray;
import org.json.JSONObject;

/** Opt-in per-account snapshots. Content is encrypted; no cloud backup or network. */
final class MaxMessageArchive {
    private final Context context;
    private final int account;
    private final File file;
    private SQLiteDatabase db;
    volatile boolean storageFailed;
    MaxMessageArchive(Context context, int account) {
        this.context = context; this.account = account;
        file = new File(context.getNoBackupFilesDir(), "max-archive-" + account + ".db");
    }
    boolean enabled() { return context.getSharedPreferences("max_local_" + account, 0).getBoolean("keep_deleted", false); }
    synchronized void enabled(boolean value) {
        if (!context.getSharedPreferences("max_local_" + account, 0).edit().putBoolean("keep_deleted", value).commit())
            throw new io.github.lubara7.tgmax.max.MaxException("MAX_STORAGE");
        if (!value) clear();
    }
    private SQLiteDatabase db() {
        if (db == null) {
            db = SQLiteDatabase.openOrCreateDatabase(file, null);
            db.execSQL("CREATE TABLE IF NOT EXISTS snapshots(chat TEXT,id TEXT,time INTEGER,body BLOB,deleted INTEGER DEFAULT 0,PRIMARY KEY(chat,id))");
        }
        return db;
    }
    synchronized void observe(long chat, Map<String, Object> message) {
        if (!enabled() || message.get("id") == null || "REMOVED".equals(message.get("status"))) return;
        try {
            byte[] raw = new JSONObject(message).toString().getBytes(StandardCharsets.UTF_8);
            if (raw.length > 1048576) { storageFailed = true; return; }
            String id = Long.toString(Maps.number(message.get("id")));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, key());
            cipher.updateAAD((chat + ":" + id).getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = cipher.doFinal(raw), body = new byte[12 + encrypted.length];
            System.arraycopy(cipher.getIV(), 0, body, 0, 12); System.arraycopy(encrypted, 0, body, 12, encrypted.length);
            db().execSQL("INSERT OR IGNORE INTO snapshots(chat,id,time,body) VALUES(?,?,?,?)", new Object[] {Long.toString(chat), id, Maps.number(message.get("time")), body});
            db().execSQL("UPDATE snapshots SET time=?,body=? WHERE chat=? AND id=? AND deleted=0", new Object[] {Maps.number(message.get("time")), body, Long.toString(chat), id});
        } catch (Exception failure) { storageFailed = true; }
    }
    synchronized Map<String, Object> retain(long chat, long id) {
        if (!enabled()) return null;
        try (Cursor c = db().rawQuery("SELECT body FROM snapshots WHERE chat=? AND id=?", new String[] {Long.toString(chat), Long.toString(id)})) {
            if (!c.moveToFirst()) return null;
            Map<String, Object> raw = decode(chat, id, c.getBlob(0));
            db().execSQL("UPDATE snapshots SET deleted=1 WHERE chat=? AND id=?", new Object[] {Long.toString(chat), Long.toString(id)});
            raw.put("maxgram_deleted", true); return raw;
        } catch (Exception failure) { storageFailed = true; return null; }
    }
    synchronized List<Map<String, Object>> deleted(long chat, long lower, long upper, int limit) {
        List<Map<String, Object>> rows = new ArrayList<>(); if (!enabled() || !file.exists()) return rows;
        try (Cursor c = db().rawQuery("SELECT id,body FROM snapshots WHERE chat=? AND deleted=1 AND time>=? AND time<=? ORDER BY time DESC LIMIT ?",
                new String[] {Long.toString(chat), Long.toString(lower), Long.toString(upper), Integer.toString(limit)})) {
            while (c.moveToNext()) { Map<String, Object> row = decode(chat, Long.parseLong(c.getString(0)), c.getBlob(1)); row.put("maxgram_deleted", true); rows.add(row); }
        } catch (Exception failure) { storageFailed = true; }
        return rows;
    }
    synchronized Map<Long, List<Long>> retainedIds() {
        Map<Long, List<Long>> rows = new LinkedHashMap<>(); if (!file.exists()) return rows;
        try (Cursor c = db().rawQuery("SELECT chat,id FROM snapshots WHERE deleted=1", null)) {
            while (c.moveToNext()) rows.computeIfAbsent(Long.parseLong(c.getString(0)), unused -> new ArrayList<>()).add(Long.parseLong(c.getString(1)));
        }
        return rows;
    }
    synchronized void forget(long chat, long id) {
        if (!file.exists()) return;
        db().execSQL("DELETE FROM snapshots WHERE chat=? AND id=?", new Object[] {Long.toString(chat), Long.toString(id)});
    }
    synchronized void clear() {
        try { if (db != null) db.close(); }
        catch (RuntimeException failure) { storageFailed = true; }
        finally { db = null; }
        if (!SQLiteDatabase.deleteDatabase(file) && file.exists()) storageFailed = true;
    }
    private Map<String, Object> decode(long chat, long id, byte[] body) throws Exception {
        if (body.length < 28) throw new IllegalStateException("Invalid archive record");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Arrays.copyOf(body, 12)));
        cipher.updateAAD((chat + ":" + id).getBytes(StandardCharsets.UTF_8));
        return object(new JSONObject(new String(cipher.doFinal(Arrays.copyOfRange(body, 12, body.length)), StandardCharsets.UTF_8)));
    }
    static Map<String, Object> object(JSONObject json) throws Exception {
        Map<String, Object> map = new LinkedHashMap<>(); java.util.Iterator<String> keys = json.keys();
        while (keys.hasNext()) { String key = keys.next(); map.put(key, value(json.get(key))); } return map;
    }
    private static Object value(Object value) throws Exception {
        if (value instanceof JSONObject) return object((JSONObject) value);
        if (value instanceof JSONArray) { List<Object> list = new ArrayList<>(); JSONArray a = (JSONArray) value; for (int i=0; i<a.length(); i++) list.add(value(a.get(i))); return list; }
        return value == JSONObject.NULL ? null : value;
    }
    private SecretKey key() throws Exception {
        String alias = context.getPackageName() + ".max-archive." + account;
        KeyStore store = KeyStore.getInstance("AndroidKeyStore"); store.load(null);
        SecretKey key = (SecretKey) store.getKey(alias, null); if (key != null) return key;
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT).setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }
}
