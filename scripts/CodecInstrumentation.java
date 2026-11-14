package io.github.lubara7.tgmax.codectest;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;
import android.util.Log;

public class CodecInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        start();
    }

    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            CodecDeviceSmoke.verify(getTargetContext().getClassLoader());
            java.util.concurrent.atomic.AtomicReference<Throwable> uiFailure = new java.util.concurrent.atomic.AtomicReference<>();
            runOnMainSync(() -> {
                try { CodecDeviceSmoke.verifyLocalFeatures(getTargetContext().getClassLoader()); }
                catch (Throwable failure) { uiFailure.set(failure); }
            });
            if (uiFailure.get() != null) throw new IllegalStateException("Local feature check failed", uiFailure.get());
            result.putString("codec_check", "passed");
            result.putString("features_check", "passed");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable failure) {
            result.putString("codec_check", "failed");
            result.putString("stack", Log.getStackTraceString(failure));
            finish(Activity.RESULT_CANCELED, result);
        }
    }
}
