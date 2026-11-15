package io.github.lubara7.tgmax.max;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;
public class MaxJsonFrameCodecTest {
    private final MaxJsonFrameCodec codec=new MaxJsonFrameCodec();
    @Test public void idsLargerThanJavascriptSafeIntegerRemainExact(){
        long id=9007199254740997L;
        MaxFrame frame=codec.decode(codec.encode(new MaxFrame(1,17,49,Maps.of("id",id,"text","😀"))));
        assertEquals(id,Maps.number(Maps.object(frame.payload).get("id")));assertEquals("😀",Maps.object(frame.payload).get("text"));
    }
    @Test public void pushesMayOmitVersionCommandAndSequence(){
        MaxFrame frame=codec.decode("{\"opcode\":128,\"payload\":{\"chatId\":1}}");
        assertEquals(0,frame.command);assertEquals(0,frame.sequence);
    }
    @Test public void malformedOrAmbiguousFramesAreRejected(){
        for(String invalid:new String[]{"{opcode:128}","{\"ver\":11,\"cmd\":1,\"opcode\":6}","{\"opcode\":128,\"opcode\":129}","{\"ver\":10,\"cmd\":1,\"seq\":1,\"opcode\":6}","{\"opcode\":128} trailing"}){
            try{codec.decode(invalid);fail(invalid);}catch(MaxException expected){}
        }
    }
    @Test public void nullPayloadAndBooleanValuesRoundTrip(){
        assertNull(codec.decode(codec.encode(new MaxFrame(1,1,1,null))).payload);
        Map<String,Object> p=Maps.object(codec.decode(codec.encode(new MaxFrame(1,2,22,Maps.of("hidden",true)))).payload);
        assertEquals(Boolean.TRUE,p.get("hidden"));
    }
}
