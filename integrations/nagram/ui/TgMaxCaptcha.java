package io.github.lubara7.tgmax.nagram;

import android.content.Context;
import android.net.Uri;
import android.webkit.CookieManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import io.github.lubara7.tgmax.max.MaxException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import org.json.JSONObject;

public final class TgMaxCaptcha extends FrameLayout {
    private static final String ORIGIN = "https://web.max.ru";
    public final CompletableFuture<String> result = new CompletableFuture<>();
    private WebView webView;

    public TgMaxCaptcha(Context context, String link, String language, boolean dark) {
        super(context);
        try {
            Uri uri = Uri.parse(link);
            String host = uri.getHost();
            if (!"https".equals(uri.getScheme()) || host == null || uri.getUserInfo() != null ||
                    uri.getPort() != -1 && uri.getPort() != 443 || !trustedHost(host))
                throw new MaxException("MAX_CAPTCHA_URL_INVALID");
            webView = new WebView(context);
            if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER))
                throw new MaxException("MAX_CAPTCHA_WEBVIEW_UPDATE");
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setAllowFileAccess(false);
            settings.setAllowContentAccess(false);
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
            settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
            webView.setWebViewClient(new WebViewClient() {
                @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    return request.isForMainFrame();
                }
                @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    if (request.isForMainFrame()) result.completeExceptionally(new MaxException("MAX_CAPTCHA_LOAD_FAILED"));
                }
            });
            WebViewCompat.addWebMessageListener(webView, "tgmaxCaptcha", Collections.singleton(ORIGIN),
                    (view, message, sourceOrigin, isMainFrame, reply) -> {
                        if (!isMainFrame || !ORIGIN.equals(sourceOrigin.toString()) || result.isDone()) return;
                        try {
                            JSONObject event = new JSONObject(message.getData());
                            switch (event.optString("type")) {
                                case "token":
                                    String token = event.optString("token");
                                    if (token.isEmpty() || token.length() > 32768) throw new IllegalArgumentException();
                                    result.complete(token);
                                    break;
                                case "cancel":
                                    result.completeExceptionally(new MaxException("MAX_CAPTCHA_CANCELLED"));
                                    break;
                                case "error":
                                    result.completeExceptionally(new MaxException("MAX_CAPTCHA_LOAD_FAILED"));
                                    break;
                            }
                        } catch (Exception invalid) {
                            result.completeExceptionally(new MaxException("MAX_CAPTCHA_RESULT_INVALID"));
                        }
                    });
            addView(webView, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            JSONObject config = new JSONObject();
            config.put("url", link);
            config.put("language", language.replace('_', '-').split("-")[0]);
            config.put("dark", dark);
            try (InputStream input = context.getAssets().open("tgmax-captcha.html")) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] block = new byte[4096];
                int count;
                while ((count = input.read(block)) != -1) bytes.write(block, 0, count);
                String page = bytes.toString("UTF-8");
                page = page.replace("__TGMAX_CONFIG__", config.toString().replace("<", "\\u003c"));
                webView.loadDataWithBaseURL(ORIGIN + "/tgmax-captcha/", page, "text/html", "UTF-8", null);
            }
        } catch (Exception | LinkageError failure) {
            result.completeExceptionally(failure instanceof MaxException ? failure : new MaxException("MAX_CAPTCHA_LOAD_FAILED", failure));
        }
    }

    private static boolean trustedHost(String host) {
        return host.equals("vk.com") || host.endsWith(".vk.com") || host.equals("vk.ru") || host.endsWith(".vk.ru") ||
                host.equals("max.ru") || host.endsWith(".max.ru") || host.equals("oneme.ru") || host.endsWith(".oneme.ru");
    }

    public void close() {
        result.completeExceptionally(new MaxException("MAX_CAPTCHA_CANCELLED"));
        if (webView != null) {
            removeView(webView);
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
    }
}
