package io.github.lubara7.tgmax.max;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Telegram offsets over a MAX timestamp window; message IDs disambiguate ties. */
public final class MaxHistoryQuery {
    public static final Comparator<Map<String, Object>> ORDER = (a, b) -> {
        int time = Long.compare(Maps.number(a.get("time")), Maps.number(b.get("time")));
        return time != 0 ? time : Long.compare(Maps.number(a.get("id")), Maps.number(b.get("id")));
    };
    public final long anchorTime, anchorId, from;
    public final boolean hasAnchor;
    public final int limit, addOffset, backward, forward;

    public MaxHistoryQuery(long time, long id, boolean anchor, int limit, int addOffset) {
        if (time < 0 || limit < 1 || limit > 200 || addOffset < -2000 || addOffset > 2000)
            throw new MaxException("MAX_HISTORY_WINDOW_INVALID");
        anchorTime = time; anchorId = id; hasAnchor = anchor; this.limit = limit; this.addOffset = addOffset;
        // Include the boundary millisecond, then exclude the anchor by (time, id).
        from = time == Long.MAX_VALUE ? time : time + 1;
        backward = Math.min(4096, limit + Math.max(0, addOffset) + 64);
        forward = addOffset < 0 ? Math.min(4096, -addOffset + 64) : 0;
    }

    public List<Map<String, Object>> select(List<Map<String, Object>> input) {
        Map<Long, Map<String, Object>> unique = new LinkedHashMap<>();
        for (Map<String, Object> raw : input) unique.put(Maps.number(raw.get("id")), raw);
        List<Map<String, Object>> rows = new ArrayList<>(unique.values()); rows.sort(ORDER.reversed());
        int position = 0;
        if (hasAnchor) {
            Map<String, Object> key = Maps.of("id", anchorId, "time", anchorTime);
            while (position < rows.size() && ORDER.compare(rows.get(position), key) >= 0) position++;
        } else if (anchorTime != Long.MAX_VALUE) {
            while (position < rows.size() && Maps.number(rows.get(position).get("time")) > anchorTime) position++;
        }
        int first = Math.max(0, Math.min(rows.size(), position + addOffset));
        return new ArrayList<>(rows.subList(first, Math.min(rows.size(), first + limit)));
    }

    public boolean boundaryNeedsExpansion(List<Map<String, Object>> rows) {
        return hasAnchor && !rows.isEmpty() && select(rows).size() < limit &&
                rows.stream().allMatch(row -> Maps.number(row.get("time")) >= anchorTime);
    }
}
