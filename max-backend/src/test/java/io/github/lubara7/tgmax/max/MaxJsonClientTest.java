package io.github.lubara7.tgmax.max;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import okhttp3.mockwebserver.*;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
public class MaxJsonClientTest {
    private final MockWebServer server=new MockWebServer();
    private final MaxJsonFrameCodec codec=new MaxJsonFrameCodec();
    private MaxClient client;
    @After public void close()throws Exception{if(client!=null)client.close();server.shutdown();}
    private MaxDevice device(){return new MaxDevice("original-web-id","ru","Android","800x360 3.0x","UTC","real-test-browser");}
    private void respond(WebSocket ws,MaxFrame request,Object value){ws.send(codec.encode(new MaxFrame(1,request.sequence,request.opcode,value)));}
    @Test public void newWebSessionUsesTextV11AndPhoneAuthOrdering()throws Exception{
        java.util.List<Integer> order=new java.util.ArrayList<>();
        server.enqueue(new MockResponse().withWebSocketUpgrade(new WebSocketListener(){
            @Override public void onMessage(WebSocket ws,String text){
                assertTrue(text.contains("\"ver\":11"));MaxFrame frame=codec.decode(text);order.add(frame.opcode);
                if(frame.opcode==6){assertEquals("original-web-id",Maps.object(frame.payload).get("deviceId"));respond(ws,frame,Maps.of());}
                else if(frame.opcode==224)respond(ws,frame,Maps.of());
                else if(frame.opcode==17){assertFalse(Maps.object(frame.payload).containsKey("mode"));respond(ws,frame,Maps.of("token","synthetic-otp","codeLength",6));}
            }
        }));server.start();client=new MaxClient(new OkHttpClient(),device(),server.url("/websocket").toString(),2000);
        assertTrue(client.usesJson());assertEquals("synthetic-otp",new MaxApi(client).requestCode("+19995550123",false).get(4,TimeUnit.SECONDS).get("token"));
        assertEquals(java.util.Arrays.asList(6,224,17),order);
    }
    @Test public void tokenLoginKeepsLongIdsAndHasNoBinaryOnlyPayload()throws Exception{
        server.enqueue(new MockResponse().withWebSocketUpgrade(new WebSocketListener(){
            @Override public void onMessage(WebSocket ws,String text){
                MaxFrame frame=codec.decode(text);
                if(frame.opcode==19){Map<String,Object> p=Maps.object(frame.payload);assertEquals("synthetic-owner-token",p.get("token"));assertFalse(p.containsKey("exp"));assertEquals(40,Maps.number(p.get("chatsCount")));respond(ws,frame,Maps.of("viewerId",9007199254740997L));}
                else respond(ws,frame,Maps.of());
            }
        }));server.start();client=new MaxClient(new OkHttpClient(),device(),server.url("/websocket").toString(),2000);
        MaxProtocolJournal journal=new MaxProtocolJournal();client.setTraceListener(journal::record);
        assertEquals(9007199254740997L,Maps.number(new MaxApi(client).login("synthetic-owner-token").get(4,TimeUnit.SECONDS).get("viewerId")));
        String log=journal.export();assertTrue(log.contains("opcode=19"));assertTrue(log.contains("JSON11"));
        assertFalse(log.contains("synthetic-owner-token"));assertFalse(log.contains("original-web-id"));assertFalse(log.contains("real-test-browser"));
    }
    @Test public void webSocketsAlsoWaitForExplicitConsent()throws Exception{
        CompletableFuture<Void> permission=new CompletableFuture<>();server.start();
        client=new MaxClient(new OkHttpClient(),device(),server.url("/websocket").toString(),2000,()->permission);
        CompletableFuture<Void> wait=client.connect();assertFalse(wait.isDone());assertEquals(0,server.getRequestCount());client.close();
        try{wait.get(1,TimeUnit.SECONDS);fail();}catch(ExecutionException expected){}assertEquals(0,server.getRequestCount());
    }
}
