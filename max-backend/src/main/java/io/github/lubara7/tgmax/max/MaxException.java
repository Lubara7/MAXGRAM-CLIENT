package io.github.lubara7.tgmax.max;

public final class MaxException extends RuntimeException {
    public final String code;

    public MaxException(String code) {
        super(code);
        this.code = code;
    }

    public MaxException(String code, Throwable cause) {
        super(code, cause);
        this.code = code;
    }
}
