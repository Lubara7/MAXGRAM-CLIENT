package io.github.lubara7.tgmax.max;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Normalizes the distinct WEB/mobile edit and deletion notifications. */
public final class MaxMessageEvent {
    public final long chat;
    public final Map<String, Object> message;
    public final List<Long> deleted;
    public final boolean edited;
    public MaxMessageEvent(int opcode, Map<String, Object> payload) {
        message = Maps.object(payload.get("message"));
        chat = Maps.number(payload.get("chatId"), Maps.number(Maps.object(payload.get("chat")).get("id"), 0));
        String status = Maps.string(message.get("status"));
        if (status.isEmpty()) status = Maps.string(payload.get("status"));
        edited = "EDITED".equals(status) || opcode == MaxOpcode.EDIT;
        deleted = new ArrayList<>();
        if (opcode == 142) {
            Object ids = payload.get("messageIds");
            if (ids instanceof List<?>) for (Object id : (List<?>) ids) deleted.add(Maps.number(id));
        } else if ("REMOVED".equals(status) && message.get("id") != null) deleted.add(Maps.number(message.get("id")));
        if (chat == 0 || deleted.isEmpty() && message.isEmpty()) throw new MaxException("MAX_MESSAGE_EVENT_INVALID");
    }
}
