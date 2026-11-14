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
            result.putString("codec_check", "passed");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable failure) {
            result.putString("codec_check", "failed");
            result.putString("stack", Log.getStackTraceString(failure));
            finish(Activity.RESULT_CANCELED, result);
        }
    }
}
