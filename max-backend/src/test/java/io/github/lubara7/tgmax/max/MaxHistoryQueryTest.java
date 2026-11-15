package io.github.lubara7.tgmax.max;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.*;

public class MaxHistoryQueryTest {
    private Map<String, Object> row(long id, long time) { return Maps.of("id", id, "time", time); }
    private List<Long> ids(List<Map<String, Object>> rows) {
        return rows.stream().map(r -> Maps.number(r.get("id"))).collect(Collectors.toList());
    }
    @Test public void doesNotSkipTheRestOfTheBoundaryMillisecond() {
        MaxHistoryQuery q = new MaxHistoryQuery(1000, 4, true, 3, 0);
        assertEquals(1001, q.from);
        assertEquals(Arrays.asList(3L, 2L, 1L), ids(q.select(Arrays.asList(row(1, 999), row(4, 1000), row(3, 1000), row(2, 1000), row(5, 1001)))));
    }
    @Test public void aroundAndForwardOffsetsKeepTelegramOrdering() {
        List<Map<String, Object>> input = Arrays.asList(row(1, 1), row(2, 2), row(3, 3), row(4, 4), row(5, 5));
        assertEquals(Arrays.asList(4L, 3L, 2L), ids(new MaxHistoryQuery(3, 3, true, 3, -2).select(input)));
        assertEquals(Arrays.asList(1L), ids(new MaxHistoryQuery(3, 3, true, 3, 1).select(input)));
    }
    @Test public void deletedAnchorAndDuplicateFramesDoNotBreakPaging() {
        MaxHistoryQuery q = new MaxHistoryQuery(3, 3, true, 2, 0);
        assertEquals(Arrays.asList(2L, 1L), ids(q.select(Arrays.asList(row(4, 4), row(2, 2), row(2, 2), row(1, 1)))));
    }
    @Test public void latestAndDateOffsetsAreDistinctFromMessageOffsets() {
        List<Map<String, Object>> input = Arrays.asList(row(1, 1), row(2, 2), row(3, 3));
        assertEquals(Arrays.asList(3L, 2L), ids(new MaxHistoryQuery(Long.MAX_VALUE, 0, false, 2, 0).select(input)));
        assertEquals(Arrays.asList(2L, 1L), ids(new MaxHistoryQuery(2, 0, false, 2, 0).select(input)));
    }    @Test public void denseBoundaryRequestsMoreInsteadOfReportingAnEmptyHistory() {
        MaxHistoryQuery q = new MaxHistoryQuery(1000, 2, true, 3, 0);
        assertTrue(q.boundaryNeedsExpansion(Arrays.asList(row(4, 1000), row(3, 1000))));
        assertFalse(q.boundaryNeedsExpansion(Arrays.asList(row(4, 1000), row(1, 999))));
    }

}
