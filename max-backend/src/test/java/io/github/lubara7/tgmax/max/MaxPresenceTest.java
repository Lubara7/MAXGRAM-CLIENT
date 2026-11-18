package io.github.lubara7.tgmax.max;

import org.junit.Test;
import static org.junit.Assert.*;

public class MaxPresenceTest {
    @Test public void serverPrivacyBucketsDoNotRevealIncludedTimestamp() {
        MaxPresence recent = new MaxPresence(Maps.of("status",2,"seen",1_700_000_000),1000);
        MaxPresence longAgo = new MaxPresence(Maps.of("status",3,"seen",1_795_000_000),1000);
        assertEquals(MaxPresence.State.RECENTLY,recent.state(1_000_000,false));
        assertEquals(MaxPresence.State.LONG_AGO,longAgo.state(1001,false));
        assertEquals(MaxPresence.State.LONG_AGO,recent.state(1001,true));
    }
    @Test public void onlineExpiresFromReceiptWithoutExtendingOnRead() {
        MaxPresence online = new MaxPresence(Maps.of("status",1,"seen",1_795_000_000),1000);
        assertEquals(MaxPresence.State.ONLINE,online.state(1001,false));
        assertEquals(1,online.remainingMillis(300999));
        assertEquals(MaxPresence.State.OFFLINE,online.state(301000,false));
        assertEquals(MaxPresence.State.LONG_AGO,online.state(1001,true));
    }
    @Test public void lastSeenUsesSecondsAndMissingDataStaysPrivate() {
        MaxPresence offline = new MaxPresence(Maps.of("status",0,"seen",1_795_000_000),0);
        assertEquals(1_795_000_000,offline.seenSeconds);
        assertEquals(MaxPresence.State.OFFLINE,offline.state(1000,false));
        assertEquals(MaxPresence.State.LONG_AGO,new MaxPresence(Maps.of("status",0,"seen",-1),0).state(0,false));
        assertEquals(MaxPresence.State.LONG_AGO,new MaxPresence(Maps.of("status",99,"seen",1_795_000_000),0).state(0,false));
    }
}
