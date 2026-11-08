package io.github.lubara7.tgmax.max;

public final class MaxFrame {
    public final int command;
    public final int sequence;
    public final int opcode;
    public final Object payload;

    public MaxFrame(int command, int sequence, int opcode, Object payload) {
        this.command = command;
        this.sequence = sequence;
        this.opcode = opcode;
        this.payload = payload;
    }

    @Override public String toString() {
        return "MaxFrame(command=" + command + ", sequence=" + sequence + ", opcode=" + opcode + ", payload=[redacted])";
    }
}
