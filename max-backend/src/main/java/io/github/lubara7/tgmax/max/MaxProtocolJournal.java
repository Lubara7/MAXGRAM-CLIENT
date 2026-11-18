package io.github.lubara7.tgmax.max;

import java.util.ArrayDeque;

/** Bounded diagnostics. Its input type cannot contain a payload or credentials. */
public final class MaxProtocolJournal {
    private static final int LIMIT = 512;
    private final ArrayDeque<Entry> entries = new ArrayDeque<>();

    public synchronized void record(Entry entry) {
        while (entries.size() >= LIMIT) entries.removeFirst();
        entries.addLast(entry);
    }

    public synchronized void clear() { entries.clear(); }

    public synchronized String export() {
        StringBuilder text = new StringBuilder("MAXGRAM protocol metadata\nPayloads, headers and credentials are not recorded.\n");
        for (Entry entry : entries) text.append(entry).append('\n');
        return text.toString();
    }

    public static final class Entry {
        public final long timeMillis;
        public final boolean incoming, json;
        public final int command, sequence, opcode, wireBytes;

        public Entry(long timeMillis, boolean incoming, boolean json, int command, int sequence, int opcode, int wireBytes) {
            this.timeMillis = timeMillis; this.incoming = incoming; this.json = json;
            this.command = command; this.sequence = sequence; this.opcode = opcode; this.wireBytes = wireBytes;
        }

        @Override public String toString() {
            return timeMillis + " " + (incoming ? "IN" : "OUT_QUEUED") + " " + (json ? "JSON11" : "BINARY10")
                    + " cmd=" + command + " seq=" + sequence + " opcode=" + opcode + " bytes=" + wireBytes;
        }
    }
}
