package io.github.lubara7.tgmax.max;

public final class MaxOpcode {
    public static final int PING = 1;
    public static final int INIT = 6;
    public static final int AUTH_REQUEST = 17;
    public static final int AUTH = 18;
    public static final int LOGIN = 19;
    public static final int LOGOUT = 20;
    public static final int REGISTER = 23;
    public static final int CONTACT_INFO = 32;
    public static final int CHAT_INFO = 48;
    public static final int HISTORY = 49;
    public static final int READ = 50;
    public static final int CHATS = 53;
    public static final int JOIN = 57;
    public static final int LEAVE = 58;
    public static final int SEARCH = 60;
    public static final int SEND = 64;
    public static final int TYPING = 65;
    public static final int DELETE = 66;
    public static final int EDIT = 67;
    public static final int SESSIONS_INFO = 96;
    public static final int SESSIONS_CLOSE = 97;
    public static final int CHECK_PASSWORD = 115;
    public static final int MESSAGE = 128;
    public static final int CHAT_UPDATE = 135;
    public static final int AUTH_CAPTCHA = 224;
    public static final int AUTH_BY_TRACK = 291;
    public static final int AUTH_CHECK_CALL = 294;

    private MaxOpcode() { }
}
