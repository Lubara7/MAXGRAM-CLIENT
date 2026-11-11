package io.github.lubara7.tgmax.max;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class MaxApi {
    private final MaxClient client;

    public MaxApi(MaxClient client) { this.client = client; }

    public CompletableFuture<Map<String, Object>> requestCode(String phone, boolean resend) {
        if (!phone.matches("\\+[1-9][0-9]{7,14}")) throw new MaxException("PHONE_NUMBER_INVALID");
        return client.connect().thenCompose(unused -> {
            Map<String, Object> payload = Maps.of("phone", phone, "type", resend ? "RESEND" : "START_AUTH",
                    "language", client.device().language);
            byte[] fingerprint = fingerprint();
            if (fingerprint != null) payload.put("mode", fingerprint);
            return client.request(MaxOpcode.AUTH_REQUEST, payload);
        });
    }

    public CompletableFuture<Map<String, Object>> verifyCode(String code, String token) {
        if (!code.matches("[0-9]{1,12}")) throw new MaxException("PHONE_CODE_INVALID");
        return client.request(MaxOpcode.AUTH, Maps.of("token", token, "verifyCode", code, "authTokenType", "CHECK_CODE"));
    }

    public CompletableFuture<Map<String, Object>> register(String token, String firstName, String lastName) {
        return client.request(MaxOpcode.REGISTER, Maps.of("token", token, "tokenType", "REGISTER",
                "firstName", firstName, "lastName", lastName));
    }

    public CompletableFuture<Map<String, Object>> checkPassword(String trackId, String password) {
        if (trackId == null || trackId.isEmpty()) throw new MaxException("PHONE_CODE_EXPIRED");
        if (password == null || password.isEmpty()) throw new MaxException("PASSWORD_HASH_INVALID");
        return client.request(MaxOpcode.CHECK_PASSWORD, Maps.of("trackId", trackId, "password", password));
    }

    public CompletableFuture<Map<String, Object>> login(String token) {
        return client.connect().thenCompose(unused -> {
            Map<String, Object> payload = Maps.of("token", token, "interactive", true, "presenceSync", -1,
                    "chatsSync", -1, "exp", Maps.of("chatsCountGroups", new byte[] {0x0b, 0x32}));
            byte[] fingerprint = fingerprint();
            if (fingerprint != null) payload.put("chatCacheFingerprint", fingerprint);
            return client.request(MaxOpcode.LOGIN, payload);
        });
    }

    public CompletableFuture<Map<String, Object>> chats(long marker, int count) {
        return client.request(MaxOpcode.CHATS, Maps.of("marker", marker, "count", Math.min(100, Math.max(1, count))));
    }

    public CompletableFuture<Map<String, Object>> chatInfo(long chatId) {
        return client.request(MaxOpcode.CHAT_INFO, Maps.of("chatIds", Collections.singletonList(chatId)));
    }

    public CompletableFuture<Map<String, Object>> contacts(List<Long> ids) {
        return client.request(MaxOpcode.CONTACT_INFO, Maps.of("contactIds", ids));
    }

    public CompletableFuture<Map<String, Object>> history(long chatId, long from, int count) {
        return client.request(MaxOpcode.HISTORY, Maps.of("chatId", chatId, "from", from, "forward", 0,
                "backward", Math.min(100, Math.max(1, count)), "getMessages", true));
    }

    public CompletableFuture<Map<String, Object>> sendText(long chatId, String text, long clientId, boolean notify, Long replyId) {
        Map<String, Object> message = Maps.of("text", text, "cid", clientId, "elements", Collections.emptyList(),
                "attaches", Collections.emptyList());
        if (replyId != null) message.put("link", Maps.of("type", "REPLY", "chatId", chatId, "messageId", replyId));
        return client.request(MaxOpcode.SEND, Maps.of("chatId", chatId, "message", message, "notify", notify));
    }

    public CompletableFuture<Map<String, Object>> edit(long chatId, long messageId, String text) {
        return client.request(MaxOpcode.EDIT, Maps.of("chatId", chatId, "messageId", messageId,
                "text", text, "elements", Collections.emptyList()));
    }

    public CompletableFuture<Map<String, Object>> delete(long chatId, List<Long> ids, boolean everyone) {
        return client.request(MaxOpcode.DELETE, Maps.of("chatId", chatId, "messageIds", ids,
                "forMe", !everyone, "itemType", "REGULAR"));
    }

    public CompletableFuture<Map<String, Object>> markRead(long chatId, long messageId, long time) {
        return client.request(MaxOpcode.READ, Maps.of("type", "READ_MESSAGE", "chatId", chatId,
                "messageId", messageId, "mark", time));
    }

    public CompletableFuture<Map<String, Object>> join(String link) {
        return client.request(MaxOpcode.JOIN, Maps.of("link", link));
    }

    public CompletableFuture<Map<String, Object>> leave(long chatId) {
        return client.request(MaxOpcode.LEAVE, Maps.of("chatId", chatId));
    }

    public CompletableFuture<Map<String, Object>> logout() { return client.request(MaxOpcode.LOGOUT, Maps.of()); }

    public CompletableFuture<Map<String, Object>> sessions() {
        return client.request(MaxOpcode.SESSIONS_INFO, Maps.of());
    }

    public CompletableFuture<Map<String, Object>> terminateOtherSessions() {
        return client.request(MaxOpcode.SESSIONS_CLOSE, Maps.of());
    }

    private byte[] fingerprint() {
        Object seed = client.handshakeData().get("callsSeed");
        if (seed == null) return null;
        String[] hashes = {
                "1684414033eb263e2c615f8b7df5ed8793850a07656304997fbf07e9e21e1e93",
                "38cff46f392dc1734c308be011c2f0d8da152a390b41063dbb2c913e3032f4b3",
                "634ecc42b246784d975f180b4fecf903df235cdf0476da47163a85630eb1a6a8"};
        byte[] result = new byte[96];
        try {
            for (int i = 0; i < hashes.length; i++) {
                byte[] prefix = new byte[32];
                for (int j = 0; j < 32; j++) prefix[j] = (byte) Integer.parseInt(hashes[i].substring(j * 2, j * 2 + 2), 16);
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                digest.update(prefix);
                digest.update(ByteBuffer.allocate(8).putLong(Maps.number(seed)).array());
                digest.update(client.device().id.getBytes(StandardCharsets.UTF_8));
                System.arraycopy(digest.digest(), 0, result, i * 32, 32);
            }
        } catch (NoSuchAlgorithmException e) { throw new AssertionError(e); }
        return result;
    }
}
