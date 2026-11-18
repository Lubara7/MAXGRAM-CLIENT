package io.github.lubara7.tgmax.telegram;

import org.telegram.messenger.NativeLoader;
import org.telegram.tgnet.NativeByteBuffer;

/** Verify storage JNI before any account starts its asynchronous storage queue. */
public final class TgMaxNative {
    private static boolean verified;
    private TgMaxNative() {}

    public static synchronized void verify() {
        if (verified) return;
        if (!NativeLoader.loaded()) {
            throw new IllegalStateException("MAXGRAM native engine did not load: " + NativeLoader.log);
        }
        NativeByteBuffer buffer = null;
        try {
            buffer = new NativeByteBuffer(12);
            if (buffer.buffer == null || !buffer.buffer.isDirect()) {
                throw new IllegalStateException("MAXGRAM storage buffer is unavailable");
            }
            buffer.writeInt32(0x4d415847);
            buffer.writeInt64(0x0102030405060708L);
            buffer.rewind();
            if (buffer.readInt32(true) != 0x4d415847 || buffer.readInt64(true) != 0x0102030405060708L) {
                throw new IllegalStateException("MAXGRAM storage buffer round trip failed");
            }
            verified = true;
        } catch (Exception failure) {
            throw new IllegalStateException("MAXGRAM storage initialization failed", failure);
        } finally {
            if (buffer != null) buffer.reuse();
        }
    }
}
