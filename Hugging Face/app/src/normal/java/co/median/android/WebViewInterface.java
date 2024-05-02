package co.median.android;

import android.webkit.JavascriptInterface;

public class WebViewInterface {

    private final WebViewListener listener;
    public WebViewInterface(WebViewListener listener) {
        this.listener = listener;
    }
    @JavascriptInterface
    public void postMessage(String message) {
        listener.onMessageReceived(message);
    }

    public interface WebViewListener {
        void onMessageReceived(String message);
    }
}
