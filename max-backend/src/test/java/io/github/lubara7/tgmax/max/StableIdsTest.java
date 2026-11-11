package io.github.lubara7.tgmax.max;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class StableIdsTest {
    @Test public void idsSurviveRestartWithoutTruncatingMaxIdentifiers() {
        Memory memory = new Memory();
        StableIds first = new StableIds(memory);
        long user = first.user(9007199254740993L), chat = first.chat(-9007199254740991L);
        first.bindDialog(-chat, -9007199254740991L);
        int message = first.message(-9007199254740991L, 9007199254740995L, 1000);
        first.flush();
        StableIds restored = new StableIds(memory);
        assertEquals(user, restored.user(9007199254740993L));
        assertEquals(9007199254740993L, restored.maxUser(user));
        assertEquals(-9007199254740991L, restored.serverChat(-chat));
        assertEquals(9007199254740995L, restored.message(message).server);
    }

    @Test public void olderPagesAndOutOfOrderPushesKeepChronologicalIds() {
        StableIds ids = new StableIds(new Memory());
        int recent = ids.message(101, 10005, 5000);
        int older = ids.message(101, 10001, 1000);
        int middle = ids.message(101, 10003, 3000);
        int newer = ids.message(101, 10006, 6000);
        assertTrue(older < middle && middle < recent && recent < newer);
        assertEquals(recent, ids.message(101, 10005, 5000));
        assertNotEquals(recent, ids.message(202, 10005, 5000));
    }

    @Test public void fullHistoryPagesFitBeforeAndBetweenExistingMessages() {
        Memory memory = new Memory();
        StableIds ids = new StableIds(memory);
        int left = ids.message(12, 500, 500);
        int right = ids.message(12, 1000, 1000);
        java.util.List<Map<String, Object>> page = new java.util.ArrayList<>();
        for (long id = 600; id > 400; id--) page.add(Maps.of("id", id, "time", id));
        ids.reserveHistory(12, page);
        ids.flush();
        StableIds restored = new StableIds(memory);
        int previous = 0;
        for (long id = 401; id <= 600; id++) {
            int local = restored.findMessage(12, id).local;
            assertTrue(local > previous && local < right);
            if (id == 500) assertEquals(left, local);
            previous = local;
        }
    }

    private static final class Memory implements StableIds.Storage {
        Map<String, String> values = new HashMap<>();
        @Override public Map<String, String> load() { return values; }
        @Override public void save(Map<String, String> entries) { values = new HashMap<>(entries); }
    }
}
