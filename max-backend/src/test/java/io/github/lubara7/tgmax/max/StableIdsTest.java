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

    private static final class Memory implements StableIds.Storage {
        Map<String, String> values = new HashMap<>();
        @Override public Map<String, String> load() { return values; }
        @Override public void save(Map<String, String> entries) { values = new HashMap<>(entries); }
    }
}
