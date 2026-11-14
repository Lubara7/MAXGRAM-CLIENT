package io.github.lubara7.tgmax.codectest;

import java.util.Map;

public class CodecDeviceSmoke {
    public static void main(String[] args) throws Exception {
        verify(CodecDeviceSmoke.class.getClassLoader());
    }

    public static void verify(ClassLoader loader) throws Exception {
        Class<?> codecClass = Class.forName("io.github.lubara7.tgmax.max.MaxFrameCodec", true, loader);
        Object codec = codecClass.getConstructor().newInstance();
        String[] fixtures = {
            "0a0100070011ff00002e28b52ffd242109010082a5746f6b656ead73796e7468657469632d6f7470aa636f64654c656e677468064cb672c4",
            "0a010007001100000026c0c7210182a5746f6b656ead73796e7468657469632d6f7470aa636f64654c656e67746806c0",
            "0a0100070011ff00002a28b52ffd000009010082a5746f6b656ead73796e7468657469632d6f7470aa636f64654c656e67746806",
            "0a010007001101000023f01282a5746f6b656ead73796e7468657469632d6f7470aa636f64654c656e67746806"
        };
        for (String fixture : fixtures) {
            byte[] bytes = new byte[fixture.length() / 2];
            for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) Integer.parseInt(fixture.substring(i * 2, i * 2 + 2), 16);
            Object frame = codecClass.getMethod("decode", byte[].class).invoke(codec, (Object) bytes);
            Map<?, ?> payload = (Map<?, ?>) frame.getClass().getField("payload").get(frame);
            if (!"synthetic-otp".equals(payload.get("token"))) throw new AssertionError("Auth payload mismatch");
            if (!(payload.get("codeLength") instanceof Number) || ((Number) payload.get("codeLength")).intValue() != 6)
                throw new AssertionError("Verification code length mismatch");
        }
        System.out.println("Published APK decoded Zstandard (known/unknown size), LZ4 and nested MessagePack auth fixtures on Android");
    }
}
