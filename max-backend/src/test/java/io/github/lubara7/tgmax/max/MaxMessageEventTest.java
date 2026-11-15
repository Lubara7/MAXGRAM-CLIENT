package io.github.lubara7.tgmax.max;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class MaxMessageEventTest {
    @Test public void webRemovedMessageIsADeletionInsteadOfAnEmptyNewMessage() {
        MaxMessageEvent e = new MaxMessageEvent(128, Maps.of("chatId", 99L, "message", Maps.of("id", "123", "status", "REMOVED")));
        assertEquals(99, e.chat); assertEquals(Arrays.asList(123L), e.deleted);
    }
    @Test public void mobileDeletionUsesChatObjectAndMessageIds() {
        MaxMessageEvent e = new MaxMessageEvent(142, Maps.of("chat", Maps.of("id", 99L), "messageIds", Arrays.asList(12L, "13")));
        assertEquals(99, e.chat); assertEquals(Arrays.asList(12L, 13L), e.deleted);
    }
    @Test public void editedMessageAndNewMessageRemainDistinct() {
        assertTrue(new MaxMessageEvent(128, Maps.of("chatId", 99L, "message", Maps.of("id", 1L, "status", "EDITED"))).edited);
        assertFalse(new MaxMessageEvent(128, Maps.of("chatId", 99L, "message", Maps.of("id", 1L))).edited);
    }
}
