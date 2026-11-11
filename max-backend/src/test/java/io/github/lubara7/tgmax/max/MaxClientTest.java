package io.github.lubara7.tgmax.max;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.ByteString;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class MaxClientTest {
    private final MockWebServer server = new MockWebServer();
    private final MaxFrameCodec codec = new MaxFrameCodec();
    private MaxClient client;

    @After public void tearDown() throws Exception {
        if (client != null) client.close();
        server.shutdown();
    }

    private MaxClient start(WebSocketListener listener, long timeout) throws Exception {
        server.enqueue(new MockResponse().withWebSocketUpgrade(listener));
        server.start();
        client = new MaxClient(new OkHttpClient(),
                new MaxDevice("synthetic-device", "en", "Android", "1080x1920", "UTC", "TG-MAX-Test"),
                server.url("/websocket").toString(), timeout);
        return client;
    }

    @Test public void handshakePrecedesAuthAndPushCannotConsumeAResponse() throws Exception {
        AtomicInteger order = new AtomicInteger();
        CountDownLatch push = new CountDownLatch(1);
        MaxClient c = start(new WebSocketListener() {
            @Override public void onMessage(WebSocket ws, ByteString bytes) {
                MaxFrame f = codec.decode(bytes.toByteArray());
                if (order.getAndIncrement() == 0) {
                    assertEquals(MaxOpcode.INIT, f.opcode);
                    respond(ws, f, Maps.of());
                } else {
                    assertEquals(MaxOpcode.AUTH_REQUEST, f.opcode);
                    assertEquals("START_AUTH", Maps.object(f.payload).get("type"));
                    ws.send(ByteString.of(codec.encode(new MaxFrame(0, f.sequence, 128, Maps.of("synthetic", true)))));
                    respond(ws, f, Maps.of("token", "synthetic-challenge"));
                }
            }
        }, 2_000);
        c.addPushListener(f -> push.countDown());
        Map<String, Object> result = new MaxApi(c).requestCode("+19995550123", false).get(4, TimeUnit.SECONDS);
        assertEquals("synthetic-challenge", result.get("token"));
        assertTrue(push.await(2, TimeUnit.SECONDS));
        assertEquals("https://web.max.ru", server.takeRequest().getHeader("Origin"));
    }

    @Test public void messageTimeoutDoesNotResendTheMessage() throws Exception {
        AtomicInteger sends = new AtomicInteger();
        MaxClient c = start(new WebSocketListener() {
            @Override public void onMessage(WebSocket ws, ByteString bytes) {
                MaxFrame f = codec.decode(bytes.toByteArray());
                if (f.opcode == MaxOpcode.INIT) respond(ws, f, Maps.of());
                else sends.incrementAndGet();
            }
        }, 120);
        try {
            new MaxApi(c).sendText(101, "synthetic text", -1, true, null).get(3, TimeUnit.SECONDS);
            fail("Missing timeout");
        } catch (ExecutionException expected) { assertTrue(root(expected) instanceof MaxException); }
        assertEquals(1, sends.get());
    }

    @Test public void wrongOpcodeIsRejectedAndDisconnectFailsPendingRequests() throws Exception {
        MaxClient c = start(new WebSocketListener() {
            @Override public void onMessage(WebSocket ws, ByteString bytes) {
                MaxFrame f = codec.decode(bytes.toByteArray());
                if (f.opcode == MaxOpcode.INIT) respond(ws, f, Maps.of());
                else ws.send(ByteString.of(codec.encode(new MaxFrame(1, f.sequence, f.opcode + 1, Maps.of()))));
            }
        }, 2_000);
        try { c.request(49, Maps.of()).get(4, TimeUnit.SECONDS); fail("Mismatched response accepted"); }
        catch (ExecutionException expected) { assertEquals("PROTOCOL_ERROR", ((MaxException) root(expected)).code); }
        assertFalse(c.isConnected());
    }

    @Test public void closeCompletesPendingFutureAndRefusesReconnect() throws Exception {
        CountDownLatch received = new CountDownLatch(1);
        MaxClient c = start(new WebSocketListener() {
            @Override public void onMessage(WebSocket ws, ByteString bytes) {
                MaxFrame f = codec.decode(bytes.toByteArray());
                if (f.opcode == MaxOpcode.INIT) respond(ws, f, Maps.of());
                else received.countDown();
            }
        }, 3_000);
        CompletableFuture<?> pending = c.request(49, Maps.of());
        assertTrue(received.await(3, TimeUnit.SECONDS));
        c.close();
        try { pending.get(2, TimeUnit.SECONDS); fail("Pending request not failed"); }
        catch (ExecutionException expected) { assertEquals("CLIENT_CLOSED", ((MaxException) root(expected)).code); }
        assertTrue(c.connect().isCompletedExceptionally());
    }

    @Test public void maxPasswordUsesTrackAndReturnsLoginTokenWithoutTelegramSrp() throws Exception {
        MaxClient c = start(new WebSocketListener() {
            @Override public void onMessage(WebSocket ws, ByteString bytes) {
                MaxFrame f = codec.decode(bytes.toByteArray());
                if (f.opcode == MaxOpcode.INIT) respond(ws, f, Maps.of());
                else {
                    assertEquals(115, f.opcode);
                    assertEquals(Maps.of("trackId", "synthetic-track", "password", "synthetic-password"), f.payload);
                    respond(ws, f, Maps.of("tokenAttrs", Maps.of("LOGIN", Maps.of("token", "synthetic-login-token"))));
                }
            }
        }, 2_000);
        Map<String, Object> result = new MaxApi(c).checkPassword("synthetic-track", "synthetic-password").get(4, TimeUnit.SECONDS);
        assertEquals("synthetic-login-token", Maps.object(Maps.object(result.get("tokenAttrs")).get("LOGIN")).get("token"));
    }

    private void respond(WebSocket ws, MaxFrame request, Object payload) {
        ws.send(ByteString.of(codec.encode(new MaxFrame(1, request.sequence, request.opcode, payload))));
    }

    private static Throwable root(Throwable e) {
        while ((e instanceof ExecutionException || e instanceof java.util.concurrent.CompletionException) && e.getCause() != null)
            e = e.getCause();
        return e;
    }
}
