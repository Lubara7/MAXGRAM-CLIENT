package io.github.lubara7.tgmax.nagram;

import io.github.lubara7.tgmax.max.Maps;
import io.github.lubara7.tgmax.max.MaxException;
import io.github.lubara7.tgmax.max.StableIds;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;

final class TgMaxModels {
    final StableIds ids;
    final Map<Long, Map<String, Object>> chats = new LinkedHashMap<>();
    final Map<Long, Map<String, Object>> contacts = new LinkedHashMap<>();
    long self;

    TgMaxModels(StableIds ids) { this.ids = ids; }

    synchronized void snapshot(Map<String, Object> data) {
        Map<String, Object> profile = Maps.object(Maps.object(data.get("profile")).get("contact"));
        if (!profile.isEmpty()) {
            self = Maps.number(profile.get("id"));
            contacts.put(self, profile);
            ids.user(self);
        }
        for (Map<String, Object> contact : Maps.objects(data.get("contacts")))
            if (contact.get("id") != null) contacts.put(Maps.number(contact.get("id")), contact);
        for (Map<String, Object> chat : Maps.objects(data.get("chats"))) {
            long id = Maps.number(chat.get("id"));
            chats.put(id, chat);
            peer(chat);
        }
    }

    static TL_account.authorizations sessions(Map<String, Object> data) {
        if (!(data.get("sessions") instanceof List)) throw new MaxException("MAX_SESSION_LIST_MISSING");
        TL_account.authorizations result = new TL_account.authorizations();
        for (Map<String, Object> row : Maps.objects(data.get("sessions"))) {
            TLRPC.TL_authorization session = new TLRPC.TL_authorization();
            session.current = Boolean.TRUE.equals(row.get("current"));
            session.flags = session.current ? 1 : 0;
            session.hash = Maps.number(row.get("id"), 0);
            session.app_name = Maps.string(row.get("client"));
            session.device_model = Maps.string(row.get("info"));
            session.country = Maps.string(row.get("location"));
            session.date_active = (int) (Maps.number(row.get("time"), 0) / 1000);
            session.platform = session.system_version = session.app_version = session.ip = session.region = "";
            result.authorizations.add(session);
        }
        return result;
    }

    synchronized TLRPC.User user(long rawId) {
        contacts.putIfAbsent(rawId, Maps.of("id", rawId));
        TLRPC.TL_user user = new TLRPC.TL_user();
        user.id = ids.user(rawId);
        user.self = rawId == self;
        Map<String, Object> contact = contacts.getOrDefault(rawId, Collections.emptyMap());
        List<Map<String, Object>> names = Maps.objects(contact.get("names"));
        Map<String, Object> name = names.isEmpty() ? contact : names.get(0);
        for (Map<String, Object> candidate : names) if ("ONEME".equals(candidate.get("type"))) name = candidate;
        user.first_name = Maps.string(name.get("firstName"));
        if (user.first_name.isEmpty()) user.first_name = Maps.string(name.get("name"));
        if (user.first_name.isEmpty()) user.first_name = "MAX " + rawId;
        user.last_name = Maps.string(name.get("lastName"));
        user.flags = 2;
        if (!user.last_name.isEmpty()) user.flags |= 4;
        user.photo = new TLRPC.TL_userProfilePhotoEmpty();
        user.flags |= 32;
        return user;
    }

    synchronized TLRPC.Chat chat(Map<String, Object> raw) {
        long maxId = Maps.number(raw.get("id"));
        String kind = Maps.string(raw.get("type"));
        TLRPC.Chat chat;
        if ("CHANNEL".equals(kind)) {
            TLRPC.TL_channel channel = new TLRPC.TL_channel();
            channel.broadcast = true;
            channel.creator = Maps.number(raw.get("owner"), 0) == self;
            chat = channel;
        } else {
            TLRPC.TL_chat group = new TLRPC.TL_chat();
            group.creator = Maps.number(raw.get("owner"), 0) == self;
            chat = group;
        }
        chat.id = ids.chat(maxId);
        chat.title = Maps.string(raw.get("title"));
        if (chat.title.isEmpty()) chat.title = "MAX";
        chat.photo = new TLRPC.TL_chatPhotoEmpty();
        chat.date = (int) (Maps.number(raw.get("created"), 0) / 1000);
        chat.version = 1;
        chat.participants_count = Math.toIntExact(Maps.number(raw.get("participantsCount"), 0));
        if (chat instanceof TLRPC.TL_channel && isAdministrator(raw)) {
            chat.admin_rights = new TLRPC.TL_chatAdminRights();
            chat.admin_rights.post_messages = true;
            chat.admin_rights.edit_messages = true;
            chat.admin_rights.delete_messages = true;
            chat.flags |= 16384;
        }
        return chat;
    }

    synchronized TLRPC.Peer peer(Map<String, Object> raw) {
        long maxChat = Maps.number(raw.get("id"));
        TLRPC.Peer peer;
        if ("DIALOG".equals(raw.get("type"))) {
            long other = 0;
            for (String id : Maps.object(raw.get("participants")).keySet()) {
                long candidate = Long.parseLong(id);
                if (candidate != self) { other = candidate; break; }
            }
            if (other == 0) other = self;
            TLRPC.TL_peerUser p = new TLRPC.TL_peerUser(); p.user_id = user(other).id; peer = p;
        } else if ("CHANNEL".equals(raw.get("type"))) {
            TLRPC.TL_peerChannel p = new TLRPC.TL_peerChannel(); p.channel_id = ids.chat(maxChat); peer = p;
        } else {
            TLRPC.TL_peerChat p = new TLRPC.TL_peerChat(); p.chat_id = ids.chat(maxChat); peer = p;
        }
        ids.bindDialog(dialogId(peer), maxChat);
        return peer;
    }

    synchronized Map<String, Object> requireChat(long rawId) {
        Map<String, Object> raw = chats.get(rawId);
        if (raw == null) throw new MaxException("MAX_UNKNOWN_PEER");
        return raw;
    }

    synchronized TLRPC.Message message(long rawChatId, Map<String, Object> raw) {
        TLRPC.TL_message message = new TLRPC.TL_message();
        long serverId = Maps.number(raw.get("id"));
        long time = Maps.number(raw.get("time"));
        Map<String, Object> rawChat = requireChat(rawChatId);
        message.id = ids.message(rawChatId, serverId, time);
        message.peer_id = peer(rawChat);
        message.dialog_id = dialogId(message.peer_id);
        long sender = Maps.number(raw.get("sender"), 0);
        message.from_id = sender == 0 ? message.peer_id : userPeer(user(sender).id);
        message.out = sender == self;
        message.post = "CHANNEL".equals(rawChat.get("type"));
        message.date = (int) (time / 1000);
        message.message = Maps.string(raw.get("text"));
        message.media = new TLRPC.TL_messageMediaEmpty();
        message.flags = 256;
        Object mark = Maps.object(rawChat.get("participants")).get(Long.toString(self));
        message.unread = !message.out && time > Maps.number(mark, Long.MAX_VALUE);
        Map<String, Object> link = Maps.object(raw.get("link"));
        if ("REPLY".equals(link.get("type"))) {
            long replyId = Maps.number(link.get("messageId"), 0);
            StableIds.MessageId reply = ids.findMessage(rawChatId, replyId);
            if (reply != null) {
                message.reply_to = new TLRPC.TL_messageReplyHeader();
                message.reply_to.reply_to_msg_id = reply.local;
                message.reply_to.flags |= 16;
                message.flags |= 8;
            }
        }
        return message;
    }

    synchronized TLRPC.messages_Dialogs dialogs() {
        TLRPC.TL_messages_dialogs response = new TLRPC.TL_messages_dialogs();
        for (Map<String, Object> raw : chats.values()) {
            if ("HIDDEN".equals(raw.get("status")) || "REMOVED".equals(raw.get("status"))) continue;
            TLRPC.TL_dialog dialog = new TLRPC.TL_dialog();
            dialog.peer = peer(raw);
            dialog.id = dialogId(dialog.peer);
            dialog.unread_count = Math.toIntExact(Maps.number(raw.get("newMessages"), 0));
            if (dialog.peer instanceof TLRPC.TL_peerChannel) dialog.pts = pts(dialog.id, false);
            dialog.notify_settings = new TLRPC.TL_peerNotifySettings();
            Map<String, Object> last = Maps.object(raw.get("lastMessage"));
            if (!last.isEmpty()) {
                TLRPC.Message message = message(Maps.number(raw.get("id")), last);
                response.messages.add(message);
                dialog.top_message = message.id;
                if (dialog.unread_count == 0) dialog.read_inbox_max_id = message.id;
                dialog.read_outbox_max_id = message.id;
            }
            if (!(dialog.peer instanceof TLRPC.TL_peerUser)) response.chats.add(chat(raw));
            response.dialogs.add(dialog);
        }
        response.users.addAll(users());
        return response;
    }

    synchronized TLRPC.messages_Messages history(long rawChatId, Map<String, Object> raw) {
        TLRPC.TL_messages_messages response = new TLRPC.TL_messages_messages();
        List<Map<String, Object>> messages = Maps.objects(raw.get("messages"));
        messages.sort((a, b) -> {
            int comparison = Long.compare(Maps.number(a.get("time")), Maps.number(b.get("time")));
            return comparison != 0 ? comparison : Long.compare(Maps.number(a.get("id")), Maps.number(b.get("id")));
        });
        for (Map<String, Object> message : messages) response.messages.add(message(rawChatId, message));
        Collections.reverse(response.messages);
        response.count = response.messages.size();
        Map<String, Object> chat = requireChat(rawChatId);
        if (!"DIALOG".equals(chat.get("type"))) response.chats.add(chat(chat));
        response.users.addAll(users());
        return response;
    }

    synchronized ArrayList<TLRPC.User> users() {
        ArrayList<TLRPC.User> users = new ArrayList<>();
        for (long raw : new ArrayList<>(contacts.keySet())) users.add(user(raw));
        return users;
    }

    static long dialogId(TLRPC.Peer peer) {
        if (peer.user_id != 0) return peer.user_id;
        return -(peer.channel_id != 0 ? peer.channel_id : peer.chat_id);
    }

    static long dialogId(TLRPC.InputPeer peer) {
        if (peer.user_id != 0) return peer.user_id;
        return -(peer.channel_id != 0 ? peer.channel_id : peer.chat_id);
    }

    static TLRPC.Peer userPeer(long id) { TLRPC.TL_peerUser peer = new TLRPC.TL_peerUser(); peer.user_id = id; return peer; }

    synchronized int pts(long dialog, boolean advance) {
        boolean channel = dialog < 0 && "CHANNEL".equals(requireChat(ids.serverChat(dialog)).get("type"));
        return ids.cursor(channel ? "channel:" + dialog : "global", advance);
    }

    private boolean isAdministrator(Map<String, Object> chat) {
        if (Maps.number(chat.get("owner"), 0) == self) return true;
        Object admins = chat.get("admins");
        if (admins instanceof List) for (Object admin : (List<?>) admins) if (Maps.number(admin) == self) return true;
        return Maps.object(chat.get("adminParticipants")).containsKey(Long.toString(self));
    }
}
