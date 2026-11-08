package io.github.lubara7.tgmax.max;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.jpountz.lz4.LZ4Exception;
import net.jpountz.lz4.LZ4Factory;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import java.nio.charset.StandardCharsets;

public final class MaxFrameCodec {
    public static final int VERSION = 10;
    public static final int HEADER = 10;
    public static final int MAX_BODY = 8 * 1024 * 1024;
    public static final int MAX_UNCOMPRESSED = 32 * 1024 * 1024;

    public byte[] encode(MaxFrame frame) {
        if (frame.command < 0 || frame.command > 3 || frame.sequence < Short.MIN_VALUE || frame.sequence > Short.MAX_VALUE ||
                frame.opcode < 0 || frame.opcode > 0xffff) throw new MaxException("INVALID_HEADER");
        try (MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
            byte[] body;
            if (frame.payload == null) body = new byte[0];
            else {
                pack(packer, frame.payload, 0);
                body = packer.toByteArray();
            }
            if (body.length > MAX_BODY) throw new MaxException("FRAME_TOO_LARGE");
            ByteBuffer buffer = ByteBuffer.allocate(HEADER + body.length);
            buffer.put((byte) VERSION).put((byte) frame.command).putShort((short) frame.sequence)
                    .putShort((short) frame.opcode).put((byte) 0).put((byte) (body.length >>> 16))
                    .put((byte) (body.length >>> 8)).put((byte) body.length).put(body);
            return buffer.array();
        } catch (IOException e) { throw new MaxException("ENCODE_FAILED", e); }
    }

    public MaxFrame decode(byte[] bytes) {
        if (bytes.length < HEADER) throw new MaxException("TRUNCATED_HEADER");
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int version = buffer.get() & 255;
        int command = buffer.get() & 255;
        int sequence = buffer.getShort();
        int opcode = buffer.getShort() & 0xffff;
        int ratio = buffer.get() & 255;
        int length = ((buffer.get() & 255) << 16) | ((buffer.get() & 255) << 8) | (buffer.get() & 255);
        if (version != VERSION || command > 3) throw new MaxException("UNSUPPORTED_FRAME");
        if (length > MAX_BODY || bytes.length != HEADER + length) throw new MaxException("INVALID_FRAME_LENGTH");
        if (length == 0) {
            if (ratio != 0) throw new MaxException("INVALID_COMPRESSION");
            return new MaxFrame(command, sequence, opcode, null);
        }
        byte[] body = new byte[length];
        buffer.get(body);
        if (ratio > 0) {
            long capacity = (long) length * ratio * 16;
            if (capacity > MAX_UNCOMPRESSED) throw new MaxException("DECOMPRESSED_FRAME_TOO_LARGE");
            try {
                body = LZ4Factory.fastestJavaInstance().safeDecompressor().decompress(body, (int) capacity);
            } catch (LZ4Exception e) { throw new MaxException("INVALID_COMPRESSION", e); }
        }
        try (MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(body)) {
            Object payload = unpack(unpacker, 0);
            if (unpacker.hasNext()) throw new MaxException("TRAILING_PAYLOAD");
            return new MaxFrame(command, sequence, opcode, payload);
        } catch (IOException | IllegalArgumentException e) { throw new MaxException("DECODE_FAILED", e); }
    }

    private void pack(MessageBufferPacker p, Object value, int depth) throws IOException {
        if (depth > 48) throw new MaxException("PAYLOAD_TOO_DEEP");
        if (value == null) p.packNil();
        else if (value instanceof Boolean) p.packBoolean((Boolean) value);
        else if (value instanceof String) p.packString((String) value);
        else if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long)
            p.packLong(((Number) value).longValue());
        else if (value instanceof Float || value instanceof Double) p.packDouble(((Number) value).doubleValue());
        else if (value instanceof byte[]) {
            byte[] b = (byte[]) value;
            p.packBinaryHeader(b.length).writePayload(b);
        } else if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            if (map.size() > 100_000) throw new MaxException("COLLECTION_TOO_LARGE");
            p.packMapHeader(map.size());
            for (Map.Entry<?, ?> e : map.entrySet()) { pack(p, e.getKey(), depth + 1); pack(p, e.getValue(), depth + 1); }
        } else if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.size() > 100_000) throw new MaxException("COLLECTION_TOO_LARGE");
            p.packArrayHeader(list.size());
            for (Object item : list) pack(p, item, depth + 1);
        } else throw new MaxException("UNSUPPORTED_VALUE");
    }

    private Object unpack(MessageUnpacker u, int depth) throws IOException {
        if (depth > 48) throw new MaxException("PAYLOAD_TOO_DEEP");
        switch (u.getNextFormat().getValueType()) {
            case NIL: u.unpackNil(); return null;
            case BOOLEAN: return u.unpackBoolean();
            case INTEGER:
                java.math.BigInteger number = u.unpackBigInteger();
                return number.bitLength() <= 63 ? number.longValue() : number.toString();
            case FLOAT: return u.unpackDouble();
            case STRING:
                int stringSize = u.unpackRawStringHeader();
                if (stringSize < 0 || stringSize > MAX_BODY) throw new MaxException("STRING_TOO_LARGE");
                return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(u.readPayload(stringSize))).toString();
            case BINARY:
                int binarySize = u.unpackBinaryHeader();
                if (binarySize < 0 || binarySize > MAX_BODY) throw new MaxException("BINARY_TOO_LARGE");
                return u.readPayload(binarySize);
            case ARRAY:
                int arraySize = u.unpackArrayHeader();
                if (arraySize < 0 || arraySize > 100_000) throw new MaxException("COLLECTION_TOO_LARGE");
                List<Object> list = new ArrayList<>();
                for (int i = 0; i < arraySize; i++) list.add(unpack(u, depth + 1));
                return list;
            case MAP:
                int mapSize = u.unpackMapHeader();
                if (mapSize < 0 || mapSize > 100_000) throw new MaxException("COLLECTION_TOO_LARGE");
                Map<String, Object> map = new LinkedHashMap<>();
                for (int i = 0; i < mapSize; i++) {
                    Object key = unpack(u, depth + 1);
                    if (!(key instanceof String) && !(key instanceof Number)) throw new MaxException("INVALID_MAP_KEY");
                    String name = key.toString();
                    if (map.containsKey(name)) throw new MaxException("DUPLICATE_MAP_KEY");
                    map.put(name, unpack(u, depth + 1));
                }
                return map;
            default: throw new MaxException("UNSUPPORTED_MSGPACK_VALUE");
        }
    }
}
