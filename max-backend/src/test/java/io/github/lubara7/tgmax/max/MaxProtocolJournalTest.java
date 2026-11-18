package io.github.lubara7.tgmax.max;

import org.junit.Test;
import static org.junit.Assert.*;

public class MaxProtocolJournalTest {
    @Test public void longRunningSessionKeepsOnlyRecentMetadataAndCanBeCleared() {
        MaxProtocolJournal journal = new MaxProtocolJournal();
        for (int i=0; i<2000; i++) journal.record(new MaxProtocolJournal.Entry(i,true,true,0,i,128,80));
        String log = journal.export();
        assertFalse(log.contains("seq=1487 "));
        assertTrue(log.contains("seq=1488 ")); assertTrue(log.contains("seq=1999 "));
        assertEquals(512, log.lines().filter(line -> line.contains(" opcode=")).count());
        journal.clear(); assertFalse(journal.export().contains("opcode="));
    }
}
