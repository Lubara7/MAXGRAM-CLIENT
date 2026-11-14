package io.github.lubara7.tgmax.max;

/** Local outbound controls; they do not change other devices or server-side presence rules. */
public final class MaxPrivacyPolicy {
    public static final MaxPrivacyPolicy DEFAULT = new MaxPrivacyPolicy(false, false, false);
    public final boolean ghost, unread, noTyping;

    public MaxPrivacyPolicy(boolean ghost, boolean unread, boolean noTyping) {
        this.ghost = ghost;
        this.unread = unread;
        this.noTyping = noTyping;
    }

    public boolean sendsRead() { return !ghost && !unread; }
    public boolean sendsTyping() { return !ghost && !noTyping; }
    public boolean sendsPresence() { return !ghost; }

    public boolean allows(int opcode) {
        return (opcode != MaxOpcode.READ || sendsRead()) && (opcode != MaxOpcode.TYPING || sendsTyping());
    }
}
