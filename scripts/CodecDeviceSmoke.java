package io.github.lubara7.tgmax.codectest;

import java.util.Map;

public class CodecDeviceSmoke {
    public static void main(String[] args) throws Exception {
        verify(CodecDeviceSmoke.class.getClassLoader());
    }

    public static void verify(ClassLoader loader) throws Exception {
        Class.forName("io.github.lubara7.tgmax.telegram.TgMaxNative", true, loader)
                .getMethod("verify").invoke(null);
        Class<?> webCodecClass = Class.forName("io.github.lubara7.tgmax.max.MaxJsonFrameCodec", true, loader);
        Object webCodec = webCodecClass.getConstructor().newInstance();
        Object webFrame = webCodecClass.getMethod("decode", String.class).invoke(webCodec,
                "{\"ver\":11,\"cmd\":1,\"seq\":7,\"opcode\":49,\"payload\":{\"id\":9007199254740997,\"text\":\"Привет 😀\"}}");
        Map<?, ?> webPayload = (Map<?, ?>) webFrame.getClass().getField("payload").get(webFrame);
        if (!Long.valueOf(9007199254740997L).equals(webPayload.get("id")) || !"Привет 😀".equals(webPayload.get("text")))
            throw new AssertionError("Packaged WEB JSON codec lost an ID or Unicode text");
        String json = (String) webCodecClass.getMethod("encode", webFrame.getClass()).invoke(webCodec, webFrame);
        if (!json.contains("9007199254740997")) throw new AssertionError("Packaged WEB JSON encoder rounded an ID");
        Class<?> codecClass = Class.forName("io.github.lubara7.tgmax.max.MaxFrameCodec", true, loader);
        Object codec = codecClass.getConstructor().newInstance();
        String[] fixtures = {
            "0a0100070011ff00002e28b52ffd242109010082a5746f6b656ead73796e7468657469632d6f7470aa636f64654c656e677468064cb672c4",
            "0a010007001100000026c0c7210182a5746f6b656ead73796e7468657469632d6f7470aa636f64654c656e67746806c0",
            "0a0100070011ff00002a28b52ffd000009010082a5746f6b656ead73796e7468657469632d6f7470aa636f64654c656e67746806",
            "0a010007001101000023f01282a5746f6b656ead73796e7468657469632d6f7470aa636f64654c656e67746806"
        };
        for (String fixture : fixtures) {
            byte[] bytes = new byte[fixture.length() / 2];
            for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) Integer.parseInt(fixture.substring(i * 2, i * 2 + 2), 16);
            Object frame = codecClass.getMethod("decode", byte[].class).invoke(codec, (Object) bytes);
            Map<?, ?> payload = (Map<?, ?>) frame.getClass().getField("payload").get(frame);
            if (!"synthetic-otp".equals(payload.get("token"))) throw new AssertionError("Auth payload mismatch");
            if (!(payload.get("codeLength") instanceof Number) || ((Number) payload.get("codeLength")).intValue() != 6)
                throw new AssertionError("Verification code length mismatch");
        }
        System.out.println("Prototype APK decoded Zstandard (known/unknown size), LZ4 and nested MessagePack auth fixtures on Android");
    }

    public static void verifyLocalFeatures(ClassLoader loader, android.content.Context context) throws Exception {
        verifySessionProfileUpgrade(loader, context);
        verifyArchive(loader, context);
        verifyCacheMigration(loader, context);
        Class.forName("io.github.lubara7.tgmax.telegram.TgMaxQrActivity", true, loader);
        Class.forName("io.github.lubara7.tgmax.telegram.TgMaxSettingsActivity", true, loader);
        Class<?> runtime = Class.forName("io.github.lubara7.tgmax.telegram.TgMaxRuntime", true, loader);
        Class<?> policy = Class.forName("io.github.lubara7.tgmax.max.MaxPrivacyPolicy", true, loader);
        Object first = runtime.getMethod("get", int.class).invoke(null, 0);
        Object second = runtime.getMethod("get", int.class).invoke(null, 1);
        Object original = runtime.getMethod("privacy").invoke(first);
        Object other = runtime.getMethod("privacy").invoke(second);
        Object ghost = policy.getConstructor(boolean.class, boolean.class, boolean.class).newInstance(true, false, false);
        try {
            runtime.getMethod("privacy", policy).invoke(first, ghost);
            Object active = runtime.getMethod("privacy").invoke(first);
            if ((Boolean) policy.getMethod("sendsRead").invoke(active) || (Boolean) policy.getMethod("sendsTyping").invoke(active) ||
                    (Boolean) policy.getMethod("sendsPresence").invoke(active)) throw new AssertionError("Ghost policy failed");
            Object unchanged = runtime.getMethod("privacy").invoke(second);
            if (unchanged != other) throw new AssertionError("Privacy crossed account boundaries");
        } finally { runtime.getMethod("privacy", policy).invoke(first, original); }
        java.lang.reflect.Field modelField = runtime.getDeclaredField("models");
        modelField.setAccessible(true);
        Object models = modelField.get(second);
        java.util.Map<String, Object> contact = new java.util.HashMap<>();
        contact.put("id", 12345L); contact.put("firstName", "Sample contact");
        contact.put("baseUrl", "https://i.oneme.ru/avatar/synthetic"); contact.put("photoId", 77L);
        java.util.Map<String, Object> group = new java.util.HashMap<>();
        group.put("id", 54321L); group.put("type", "CHAT"); group.put("title", "Sample group");
        group.put("baseIconUrl", "https://i.oneme.ru/group/synthetic");
        java.util.Map<String, Object> snapshot = new java.util.HashMap<>();
        snapshot.put("contacts", java.util.Collections.singletonList(contact));
        snapshot.put("chats", java.util.Collections.singletonList(group));
        java.lang.reflect.Method update = models.getClass().getDeclaredMethod("snapshot", java.util.Map.class);
        update.setAccessible(true); update.invoke(models, snapshot);
        java.lang.reflect.Method getUser = models.getClass().getDeclaredMethod("user", long.class);
        getUser.setAccessible(true);
        Object user = getUser.invoke(models, 12345L);
        if (!"Sample contact".equals(user.getClass().getField("first_name").get(user))) throw new AssertionError("MAX display name mismatch");
        java.lang.reflect.Method presence = models.getClass().getDeclaredMethod("presence",long.class,Map.class); presence.setAccessible(true);
        java.util.Map<String,Object> status = new java.util.HashMap<>(); status.put("seen",1795000000L); status.put("status",2);
        presence.invoke(models,12345L,status);
        Object recent = getUser.invoke(models,12345L);
        if (!recent.getClass().getField("status").get(recent).getClass().getSimpleName().equals("TL_userStatusRecently")) throw new AssertionError("MAX privacy status was replaced with an exact timestamp");
        java.lang.reflect.Field blockedField = models.getClass().getDeclaredField("blocked"); blockedField.setAccessible(true);
        @SuppressWarnings("unchecked") java.util.Set<Long> blocked = (java.util.Set<Long>) blockedField.get(models);
        blocked.add(12345L);
        try {
            Object hidden = getUser.invoke(models,12345L);
            if (!hidden.getClass().getField("status").get(hidden).getClass().getSimpleName().equals("TL_userStatusEmpty")) throw new AssertionError("Blocked profile exposed a recent status");
        } finally { blocked.remove(12345L); }
        long localUser=((Number)user.getClass().getField("id").get(user)).longValue();
        if(((Number)runtime.getMethod("rawUserId",long.class).invoke(second,localUser)).longValue()!=12345L)throw new AssertionError("Displayed profile ID is not the MAX ID");
        java.lang.reflect.Method authenticated=runtime.getDeclaredMethod("authenticated",Class.forName("org.telegram.tgnet.TLObject",true,loader));authenticated.setAccessible(true);
        Object contactsRequest=Class.forName("org.telegram.tgnet.TLRPC$TL_contacts_getContacts",true,loader).getConstructor().newInstance();
        Object contactsResult=((java.util.concurrent.CompletableFuture<?>)authenticated.invoke(second,contactsRequest)).get();
        if(((java.util.List<?>)contactsResult.getClass().getField("contacts").get(contactsResult)).isEmpty())throw new AssertionError("Contacts response has users but no contact rows");
        Class<?> image = Class.forName("org.telegram.messenger.ImageLocation", true, loader);
        int small = image.getField("TYPE_SMALL").getInt(null);
        Object userImage = image.getMethod("getForUser", int.class, Class.forName("org.telegram.tgnet.TLRPC$User", true, loader), int.class)
                .invoke(null, 1, user, small);
        if (!contact.get("baseUrl").equals(image.getField("path").get(userImage))) throw new AssertionError("MAX avatar URL mismatch");
        java.lang.reflect.Method getChat = models.getClass().getDeclaredMethod("chat", java.util.Map.class);
        getChat.setAccessible(true);
        Object chat = getChat.invoke(models, group);
        Object groupImage = image.getMethod("getForChat", int.class, Class.forName("org.telegram.tgnet.TLRPC$Chat", true, loader), int.class)
                .invoke(null, 1, chat, small);
        if (!group.get("baseIconUrl").equals(image.getField("path").get(groupImage))) throw new AssertionError("MAX group avatar URL mismatch");
        System.out.println("Prototype APK loaded MAX QR/local settings and isolated privacy between accounts");
    }

    private static void verifySessionProfileUpgrade(ClassLoader loader, android.content.Context context) throws Exception {
        int account = 10011;
        Class<?> type = Class.forName("io.github.lubara7.tgmax.telegram.MaxLocalState",true,loader);
        java.lang.reflect.Constructor<?> constructor = type.getDeclaredConstructor(android.content.Context.class,int.class); constructor.setAccessible(true);
        Object state = constructor.newInstance(context,account);
        java.lang.reflect.Method profile = type.getDeclaredMethod("profileVersion",String.class), session = type.getDeclaredMethod("session",long.class,String.class), clear = type.getDeclaredMethod("clear");
        for (java.lang.reflect.Method m : new java.lang.reflect.Method[]{profile,session,clear}) m.setAccessible(true);
        android.content.SharedPreferences prefs = context.getSharedPreferences("max_device_"+account,0);
        try {
            clear.invoke(state); prefs.edit().clear().putString("app_version","26.11.6").commit();
            if (!"26.11.8".equals(profile.invoke(state,"26.11.8"))) throw new AssertionError("Unbound default profile did not update");
            prefs.edit().putString("app_version","26.11.6").putBoolean("profile_pinned",true).commit();
            if (!"26.11.6".equals(profile.invoke(state,"26.11.8"))) throw new AssertionError("Explicit profile choice changed");
            prefs.edit().putBoolean("profile_pinned",false).commit();
            session.invoke(state,12345L,"synthetic-owned-token");
            if (!"26.11.6".equals(profile.invoke(state,"26.11.8"))) throw new AssertionError("Token-bound profile changed");
            prefs.edit().remove("app_version").remove("legacy_profile").commit();
            if (!"26.8.8".equals(profile.invoke(state,"26.11.8")) || !prefs.getBoolean("legacy_profile",false)) throw new AssertionError("Old session lost its legacy profile");
        } finally {
            clear.invoke(state); prefs.edit().clear().commit();
            java.security.KeyStore keys=java.security.KeyStore.getInstance("AndroidKeyStore");keys.load(null);
            keys.deleteEntry(context.getPackageName()+".max-session."+account);
        }
    }
    private static void verifyArchive(ClassLoader loader, android.content.Context context) throws Exception {
        Class<?> type = Class.forName("io.github.lubara7.tgmax.telegram.MaxMessageArchive", true, loader);
        java.lang.reflect.Constructor<?> constructor = type.getDeclaredConstructor(android.content.Context.class, int.class); constructor.setAccessible(true);
        Object store = constructor.newInstance(context, 10007), other = constructor.newInstance(context, 10008);
        java.lang.reflect.Method enabled = type.getDeclaredMethod("enabled", boolean.class), observe = type.getDeclaredMethod("observe", long.class, Map.class),
                retain = type.getDeclaredMethod("retain", long.class, long.class), clear = type.getDeclaredMethod("clear");
        for (java.lang.reflect.Method m : new java.lang.reflect.Method[] {enabled, observe, retain, clear}) m.setAccessible(true);
        Map<String,Object> message = new java.util.HashMap<>(); message.put("id", 101L); message.put("time", 1234567L); message.put("text", "private archive fixture");
        try {
            enabled.invoke(store, false); observe.invoke(store, 99L, message);
            if (retain.invoke(store, 99L, 101L) != null) throw new AssertionError("Retention enabled by default");
            enabled.invoke(store, true); enabled.invoke(other, true); observe.invoke(store, 99L, message);
            Map<?,?> copy = (Map<?,?>) retain.invoke(store, 99L, 101L);
            if (copy == null || !Boolean.TRUE.equals(copy.get("maxgram_deleted")) || !message.get("text").equals(copy.get("text"))) throw new AssertionError("Archive round trip failed");
            if (retain.invoke(other, 99L, 101L) != null || retain.invoke(store, 99L, 202L) != null) throw new AssertionError("Archive crossed account boundary or invented a copy");
            byte[] bytes = java.nio.file.Files.readAllBytes(new java.io.File(context.getNoBackupFilesDir(), "max-archive-10007.db").toPath());
            if (new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1).contains("private archive fixture")) throw new AssertionError("Archive contains cleartext message body");
            enabled.invoke(store, false); enabled.invoke(store, true);
            if (retain.invoke(store, 99L, 101L) != null) throw new AssertionError("Disable kept deleted copies");
        } finally { enabled.invoke(store, false); enabled.invoke(other, false); clear.invoke(store); clear.invoke(other); }
    }
    private static void sql(Object db, String sql) throws Exception {
        Object statement = db.getClass().getMethod("executeFast", String.class).invoke(db, sql);
        try { statement.getClass().getMethod("stepThis").invoke(statement); }
        finally { statement.getClass().getMethod("dispose").invoke(statement); }
    }
    private static int integer(Object db, String sql) throws Exception {
        return ((Number) db.getClass().getMethod("executeInt", String.class, Object[].class).invoke(db, sql, new Object[0])).intValue();
    }
    private static void verifyCacheMigration(ClassLoader loader, android.content.Context context) throws Exception {
        Class<?> database = Class.forName("org.telegram.SQLite.SQLiteDatabase", true, loader);
        Class<?> migration = Class.forName("io.github.lubara7.tgmax.telegram.TgMaxCacheMigration", true, loader);
        for (int version : new int[] {0,179,180,181}) {
            java.io.File path = java.io.File.createTempFile("maxgram-migration-", ".db", context.getCacheDir()); path.delete();
            Object db = database.getConstructor(String.class).newInstance(path.getAbsolutePath());
            try {
                sql(db, "CREATE TABLE messages_v2(mid INTEGER PRIMARY KEY,data TEXT)"); sql(db, "INSERT INTO messages_v2 VALUES(42,'keep-message')");
                sql(db, "PRAGMA user_version="+version);
                if (version==180) {
                    sql(db, "CREATE TABLE widget_state(uid INTEGER,mid INTEGER,height INTEGER,local TEXT,PRIMARY KEY(uid,mid))");
                    sql(db, "INSERT INTO widget_state VALUES(1,42,64,'keep-widget')");
                    sql(db, "CREATE TABLE widget_heights(uid INTEGER,mid INTEGER,width INTEGER,height INTEGER,date INTEGER,PRIMARY KEY(uid,mid,width))");
                    sql(db, "INSERT INTO widget_heights VALUES(1,42,100,64,123)");
                }
                migration.getMethod("ensureCurrent",database).invoke(null,db);
                if (version==0) sql(db,"PRAGMA user_version=181"); else migration.getMethod("migrate",database,int.class).invoke(null,db,version);
                migration.getMethod("ensureCurrent",database).invoke(null,db);
                if (integer(db,"PRAGMA user_version")!=181 || integer(db,"SELECT COUNT(*) FROM messages_v2 WHERE data='keep-message'")!=1) throw new AssertionError("Lost cache history at "+version);
                if (version==180 && (integer(db,"SELECT COUNT(*) FROM widget_state WHERE local='keep-widget'")!=1 || integer(db,"SELECT COUNT(*) FROM maxgram_widget_state_180_backup WHERE height=64")!=1 || integer(db,"SELECT COUNT(*) FROM widget_heights WHERE anchor=0")!=1)) throw new AssertionError("Lost widget cache");
            } finally { database.getMethod("close").invoke(db); path.delete(); }
        }
    }

}
