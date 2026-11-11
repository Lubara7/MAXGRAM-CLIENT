package io.github.lubara7.tgmax.max;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.TreeMap;

public final class StableIds {
    public interface Storage {
        Map<String, String> load();
        void save(Map<String, String> entries);
    }

    public static final class MessageId {
        public final long chat;
        public final long server;
        public final long time;
        public final int local;
        MessageId(long chat, long server, long time, int local) {
            this.chat = chat; this.server = server; this.time = time; this.local = local;
        }
    }

    private final Storage storage;
    private final Map<String, String> entries;
    private final Map<Integer, MessageId> messagesByLocal = new HashMap<>();
    private final TreeMap<Key, MessageId> ordered = new TreeMap<>();
    private boolean dirty;

    public StableIds(Storage storage) {
        this.storage = storage;
        entries = new HashMap<>(storage.load());
        for (Map.Entry<String, String> e : entries.entrySet()) {
            if (!e.getKey().startsWith("m:")) continue;
            String[] k = e.getKey().split(":"), v = e.getValue().split(":");
            if (k.length != 3 || v.length != 2) throw new MaxException("INVALID_ID_STORE");
            MessageId id = new MessageId(Long.parseLong(k[1]), Long.parseLong(k[2]), Long.parseLong(v[0]), Integer.parseInt(v[1]));
            if (id.local <= 0 || messagesByLocal.put(id.local, id) != null) throw new MaxException("INVALID_ID_STORE");
            ordered.put(new Key(id.time, id.server, id.chat), id);
        }
    }

    public synchronized long user(long server) { return allocate("u:", server); }
    public synchronized long chat(long server) { return allocate("c:", server); }
    public synchronized long maxUser(long local) { return reverse("u:", local); }
    public synchronized long maxChat(long local) { return reverse("c:", local); }

    public synchronized void bindDialog(long dialogId, long maxChat) {
        String key = "d:" + dialogId;
        String previous = entries.put(key, Long.toString(maxChat));
        dirty |= !Long.toString(maxChat).equals(previous);
    }

    public synchronized long serverChat(long dialogId) {
        String value = entries.get("d:" + dialogId);
        if (value == null) throw new MaxException("MAX_UNKNOWN_PEER");
        return Long.parseLong(value);
    }

    public synchronized int message(long chat, long server, long time) {
        String key = "m:" + chat + ":" + server;
        String existing = entries.get(key);
        if (existing != null) return Integer.parseInt(existing.substring(existing.indexOf(':') + 1));
        Key orderKey = new Key(time, server, chat);
        Map.Entry<Key, MessageId> lower = ordered.lowerEntry(orderKey), upper = ordered.higherEntry(orderKey);
        long local;
        if (lower == null && upper == null) local = 1L << 30;
        else if (lower == null) local = (long) upper.getValue().local - 1024;
        else if (upper == null) local = (long) lower.getValue().local + 1024;
        else local = ((long) lower.getValue().local + upper.getValue().local) / 2;
        if (local <= 0 || local >= Integer.MAX_VALUE || messagesByLocal.containsKey((int) local))
            throw new MaxException("MAX_MESSAGE_INDEX_REQUIRES_RESYNC");
        storeMessage(orderKey, (int) local);
        return (int) local;
    }

    public synchronized void reserveHistory(long chat, List<Map<String, Object>> messages) {
        List<Key> pending = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (Map<String, Object> raw : messages) {
            long server = Maps.number(raw.get("id"));
            if (seen.add(server) && findMessage(chat, server) == null)
                pending.add(new Key(Maps.number(raw.get("time")), server, chat));
        }
        pending.sort(Key::compareTo);
        int[] allocated = new int[pending.size()];
        for (int start = 0; start < pending.size();) {
            Key first = pending.get(start);
            Map.Entry<Key, MessageId> lower = ordered.lowerEntry(first);
            Map.Entry<Key, MessageId> upper = ordered.higherEntry(first);
            int end = start + 1;
            while (end < pending.size() && (upper == null || pending.get(end).compareTo(upper.getKey()) < 0)) end++;
            long lo = lower == null ? 0 : lower.getValue().local;
            long hi = upper == null ? Integer.MAX_VALUE : upper.getValue().local;
            int count = end - start;
            if (hi - lo - 1 < count) throw new MaxException("MAX_MESSAGE_INDEX_REQUIRES_RESYNC");
            long step = Math.max(1, (hi - lo) / (count + 1L));
            if (lower == null || upper == null) step = Math.min(1024, step);
            long next = lo + step;
            if (lower == null) next = upper == null ? (hi - step * (count - 1L)) / 2 : hi - step * count;
            for (int i = start; i < end; i++, next += step) allocated[i] = (int) next;
            start = end;
        }
        for (int i = 0; i < pending.size(); i++) storeMessage(pending.get(i), allocated[i]);
    }

    private void storeMessage(Key key, int local) {
        MessageId id = new MessageId(key.chat, key.server, key.time, local);
        messagesByLocal.put(local, id);
        ordered.put(key, id);
        entries.put("m:" + key.chat + ":" + key.server, key.time + ":" + local);
        dirty = true;
    }

    public synchronized MessageId message(int local) {
        MessageId id = messagesByLocal.get(local);
        if (id == null) throw new MaxException("MAX_UNKNOWN_MESSAGE");
        return id;
    }

    public synchronized MessageId findMessage(long chat, long server) {
        String value = entries.get("m:" + chat + ":" + server);
        return value == null ? null : messagesByLocal.get(Integer.parseInt(value.substring(value.indexOf(':') + 1)));
    }

    public synchronized void flush() {
        if (dirty) { storage.save(new HashMap<>(entries)); dirty = false; }
    }

    public synchronized int cursor(String name, boolean advance) {
        String key = "cursor:" + name;
        int value = Integer.parseInt(entries.getOrDefault(key, "0"));
        if (advance) {
            value = Math.addExact(value, 1);
            entries.put(key, Integer.toString(value));
            dirty = true;
        }
        return value;
    }

    private long allocate(String prefix, long server) {
        if (server == 0) throw new MaxException("MAX_INVALID_ID");
        String key = prefix + server;
        String existing = entries.get(key);
        if (existing != null) return Long.parseLong(existing);
        String counter = "next:" + prefix;
        long next = Long.parseLong(entries.getOrDefault(counter, "1"));
        if (next >= Integer.MAX_VALUE) throw new MaxException("MAX_ID_SPACE_EXHAUSTED");
        entries.put(key, Long.toString(next));
        entries.put(counter, Long.toString(next + 1));
        dirty = true;
        return next;
    }

    private long reverse(String prefix, long local) {
        for (Map.Entry<String, String> e : entries.entrySet())
            if (e.getKey().startsWith(prefix) && e.getValue().equals(Long.toString(local)))
                return Long.parseLong(e.getKey().substring(prefix.length()));
        throw new MaxException("MAX_UNKNOWN_PEER");
    }

    private static final class Key implements Comparable<Key> {
        final long time, server, chat;
        Key(long time, long server, long chat) { this.time = time; this.server = server; this.chat = chat; }
        @Override public int compareTo(Key b) {
            int c = Long.compare(time, b.time);
            if (c == 0) c = Long.compare(server, b.server);
            if (c == 0) c = Long.compare(chat, b.chat);
            return c;
        }
    }
}
