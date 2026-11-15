package io.github.lubara7.tgmax.max;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** WEB v11; integral IDs never pass through a floating-point number. */
public final class MaxJsonFrameCodec {
    private final Gson json=new GsonBuilder().serializeNulls().disableHtmlEscaping().create();
    public String encode(MaxFrame frame){
        String value=json.toJson(Maps.of("ver",11,"cmd",frame.command,"seq",frame.sequence,"opcode",frame.opcode,"payload",frame.payload));
        if(value.length()>MaxFrameCodec.MAX_BODY)throw new MaxException("FRAME_TOO_LARGE");return value;
    }
    public MaxFrame decode(String text){
        if(text.length()>MaxFrameCodec.MAX_BODY)throw new MaxException("FRAME_TOO_LARGE");
        try(JsonReader reader=new JsonReader(new StringReader(text))){
            reader.setStrictness(Strictness.STRICT);Object value=read(reader,0);
            if(reader.peek()!=JsonToken.END_DOCUMENT||!(value instanceof Map))throw invalid();
            Map<String,Object> frame=Maps.object(value);
            if(frame.containsKey("ver")&&Maps.number(frame.get("ver"))!=11)throw new MaxException("UNSUPPORTED_FRAME");
            long cmd=Maps.number(frame.get("cmd"),0),opcode=Maps.number(frame.get("opcode"));
            long seq=cmd==0?Maps.number(frame.get("seq"),0):Maps.number(frame.get("seq"));
            if(cmd<0||cmd>3||seq<Integer.MIN_VALUE||seq>Integer.MAX_VALUE||opcode<0||opcode>65535)throw invalid();
            return new MaxFrame((int)cmd,(int)seq,(int)opcode,frame.get("payload"));
        }catch(MaxException expected){throw expected;}catch(Exception failure){throw invalid();}
    }
    private Object read(JsonReader reader,int depth)throws Exception{
        if(depth>32)throw invalid();
        switch(reader.peek()){
            case BEGIN_OBJECT:
                Map<String,Object> map=new LinkedHashMap<>();reader.beginObject();
                while(reader.hasNext()){String key=reader.nextName();if(map.containsKey(key))throw invalid();map.put(key,read(reader,depth+1));}
                reader.endObject();return map;
            case BEGIN_ARRAY:
                ArrayList<Object> list=new ArrayList<>();reader.beginArray();while(reader.hasNext())list.add(read(reader,depth+1));reader.endArray();return list;
            case NUMBER:
                String value=reader.nextString();if(value.matches("-?[0-9]+"))return Long.parseLong(value);
                double n=Double.parseDouble(value);if(!Double.isFinite(n))throw invalid();return n;
            case STRING:return reader.nextString();
            case BOOLEAN:return reader.nextBoolean();
            case NULL:reader.nextNull();return null;
            default:throw invalid();
        }
    }
    private static MaxException invalid(){return new MaxException("INVALID_JSON_FRAME");}
}
