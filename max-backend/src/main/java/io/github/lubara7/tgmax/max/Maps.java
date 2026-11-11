package io.github.lubara7.tgmax.max;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Maps {
    private Maps() { }

    public static Map<String, Object> of(Object... pairs) {
        if (pairs.length % 2 != 0) throw new IllegalArgumentException("Unpaired map entry");
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) map.put((String) pairs[i], pairs[i + 1]);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> object(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Collections.emptyMap();
    }

    public static List<Map<String, Object>> objects(Object value) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (value instanceof List) for (Object e : (List<?>) value) if (e instanceof Map) result.add(object(e));
        return result;
    }

    public static String string(Object value) { return value == null ? "" : value.toString(); }
    public static long number(Object value) {
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long)
            return ((Number) value).longValue();
        if (value instanceof String) {
            try { return Long.parseLong((String) value); } catch (NumberFormatException ignored) { }
        }
        throw new MaxException("INVALID_INTEGER");
    }
    public static long number(Object value, long fallback) {
        if (value == null) return fallback;
        return number(value);
    }
}
