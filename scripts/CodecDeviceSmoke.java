package io.github.lubara7.tgmax.codectest;

import java.util.Map;

public class CodecDeviceSmoke {
    public static void main(String[] args) throws Exception {
        verify(CodecDeviceSmoke.class.getClassLoader());
    }

    public static void verify(ClassLoader loader) throws Exception {
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
        System.out.println("Published APK decoded Zstandard (known/unknown size), LZ4 and nested MessagePack auth fixtures on Android");
    }

    public static void verifyLocalFeatures(ClassLoader loader) throws Exception {
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
        System.out.println("Published APK loaded MAX QR/local settings and isolated privacy between accounts");
    }
}
