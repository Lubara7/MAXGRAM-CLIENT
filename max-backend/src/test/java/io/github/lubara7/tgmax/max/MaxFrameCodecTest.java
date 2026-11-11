package io.github.lubara7.tgmax.max;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import net.jpountz.lz4.LZ4Factory;
import org.junit.Test;
import static org.junit.Assert.*;

public class MaxFrameCodecTest {
    private final MaxFrameCodec codec = new MaxFrameCodec();

    @Test public void knownMessagePackFramePreservesLongIds() {
        byte[] vector = hex("0a01000700350000000d81a26964cf0020000000000001");
        MaxFrame frame = codec.decode(vector);
        assertEquals(53, frame.opcode);
        assertEquals(7, frame.sequence);
        assertEquals(9007199254740993L, Maps.number(Maps.object(frame.payload).get("id")));
        assertArrayEquals(vector, codec.encode(frame));
    }

    @Test public void emptyPayloadHasTenByteHeader() {
        assertArrayEquals(hex("0a010003000100000000"), codec.encode(new MaxFrame(1, 3, 1, null)));
    }

    @Test public void compressedFrameUsesKometLz4Ratio() {
        byte[] original = codec.encode(new MaxFrame(1, 4, 49, Maps.of("text", String.join("", Collections.nCopies(100, "synthetic ")))));
        byte[] body = Arrays.copyOfRange(original, 10, original.length);
        byte[] compressed = LZ4Factory.fastestJavaInstance().fastCompressor().compress(body);
        int ratio = (body.length + compressed.length * 16 - 1) / (compressed.length * 16);
        ByteBuffer b = ByteBuffer.allocate(10 + compressed.length);
        b.put(new byte[] {10, 1, 0, 4, 0, 49, (byte) ratio, (byte) (compressed.length >>> 16),
                (byte) (compressed.length >>> 8), (byte) compressed.length}).put(compressed);
        assertEquals(Maps.object(codec.decode(original).payload), Maps.object(codec.decode(b.array()).payload));
    }

    @Test public void malformedLengthVersionAndHugeCollectionsAreRejected() {
        reject(hex("0a010003000100000001"));
        reject(hex("0b010003000100000000"));
        reject(hex("0a010003000100000005dd7fffffff"));
        reject(hex("0a010003000100000005db7fffffff"));
        reject(hex("0a010003000100000002c0c0"));
    }

    @Test public void duplicateKeysAndInvalidUtf8AreRejected() {
        reject(hex("0a01000300010000000782a16101a16102"));
        reject(hex("0a010003000100000002a1ff"));
    }

    private void reject(byte[] bytes) {
        try { codec.decode(bytes); fail("Invalid frame accepted"); }
        catch (MaxException expected) { }
    }

    private static byte[] hex(String text) {
        byte[] data = new byte[text.length() / 2];
        for (int i = 0; i < data.length; i++) data[i] = (byte) Integer.parseInt(text.substring(i * 2, i * 2 + 2), 16);
        return data;
    }
}
