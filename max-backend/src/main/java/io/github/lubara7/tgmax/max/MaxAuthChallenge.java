package io.github.lubara7.tgmax.max;

import java.util.Map;

public final class MaxAuthChallenge {
    public final String token;
    public final MaxCallOut.Params call;
    public final int codeLength;
    public final int resendSeconds;

    public MaxAuthChallenge(Map<String, Object> data) {
        token = Maps.string(data.get("token"));
        MaxCallOut.Params parsed = null;
        if (data.get("callOutParams") != null) {
            try { parsed = new MaxCallOut.Params(data.get("callOutParams")); }
            catch (MaxException invalidOptionalCall) {
                if (token.isEmpty()) throw new MaxException("MAX_CALL_PARAMS_INVALID", invalidOptionalCall);
            }
        }
        call = parsed;
        if (call != null) {
            codeLength = 0;
            resendSeconds = 0;
            return;
        }
        if (token.isEmpty()) throw new MaxException("MAX_AUTH_TOKEN_MISSING");
        long length = Maps.number(data.get("codeLength"), 6);
        if (length < 1 || length > 12) throw new MaxException("MAX_CODE_LENGTH_INVALID");
        codeLength = (int) length;
        long millis = Maps.number(data.get("altActionDuration"), 60_000);
        resendSeconds = (int) ((Math.min(3_600_000, Math.max(1, millis)) + 999) / 1000);
    }
}
