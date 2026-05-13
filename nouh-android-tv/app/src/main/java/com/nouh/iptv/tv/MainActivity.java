package com.nouh.iptv.tv;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {

    private static final String TAG = "NOUHIPTVtv";
    private WebView webView;
    private FrameLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
            hideSystemUI();

            container = new FrameLayout(this);
            setContentView(container);

            initWebView();

        } catch (Exception e) {
            Log.e(TAG, "onCreate: " + e.getMessage(), e);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        try {
            if (webView != null) {
                container.removeView(webView);
                webView.destroy();
                webView = null;
            }

            webView = new WebView(this);
            webView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ));
            webView.setBackgroundColor(0xFF08020a);

            WebSettings ws = webView.getSettings();
            ws.setJavaScriptEnabled(true);
            ws.setDomStorageEnabled(true);
            ws.setDatabaseEnabled(true);
            ws.setMediaPlaybackRequiresUserGesture(false);
            ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            ws.setAllowFileAccess(true);
            ws.setAllowFileAccessFromFileURLs(true);
            ws.setAllowUniversalAccessFromFileURLs(true);
            ws.setAllowContentAccess(true);
            ws.setLoadWithOverviewMode(true);
            ws.setUseWideViewPort(true);
            ws.setCacheMode(WebSettings.LOAD_NO_CACHE);
            ws.setSupportZoom(false);
            ws.setBuiltInZoomControls(false);
            ws.setDisplayZoomControls(false);
            ws.setJavaScriptCanOpenWindowsAutomatically(true);
            ws.setUserAgentString(
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36"
            );

            CookieManager cm = CookieManager.getInstance();
            cm.setAcceptCookie(true);
            cm.setAcceptThirdPartyCookies(webView, true);

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                    return false;
                }
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    // Focus first nav element
                    view.evaluateJavascript(
                        "(function(){ var f=document.querySelector('.ni,.pc'); " +
                        "if(f){f.tabIndex=0;f.focus();} })()", null
                    );
                }
                @Override
                public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                    Log.e(TAG, "Render process gone — restarting");
                    runOnUiThread(() -> initWebView());
                    return true;
                }
            });

            webView.setWebChromeClient(new WebChromeClient() {
                private View customView;
                private CustomViewCallback cb;
                @Override
                public void onShowCustomView(View view, CustomViewCallback callback) {
                    customView = view; cb = callback;
                    container.addView(customView, new FrameLayout.LayoutParams(-1, -1));
                    webView.setVisibility(View.GONE);
                }
                @Override
                public void onHideCustomView() {
                    if (customView != null) { container.removeView(customView); customView = null; }
                    if (cb != null) { cb.onCustomViewHidden(); cb = null; }
                    webView.setVisibility(View.VISIBLE);
                }
            });

            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            container.addView(webView);

            // Load HTML content directly via loadDataWithBaseURL
            // This is the most compatible method for Android TV 11
            loadHtmlFromAssets();

        } catch (Exception e) {
            Log.e(TAG, "initWebView: " + e.getMessage(), e);
        }
    }

    private void loadHtmlFromAssets() {
        try {
            InputStream is = getAssets().open("www/index.html");
            String html = readStream(is);
            // Use file:///android_asset as base URL so relative paths work
            webView.loadDataWithBaseURL(
                "file:///android_asset/www/",
                html,
                "text/html",
                "UTF-8",
                null
            );
            Log.d(TAG, "HTML loaded via loadDataWithBaseURL, size=" + html.length());
        } catch (Exception e) {
            Log.e(TAG, "loadHtmlFromAssets: " + e.getMessage(), e);
            // Last resort fallback
            webView.loadUrl("file:///android_asset/www/index.html");
        }
    }

    private String readStream(InputStream is) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] tmp = new byte[8192];
        int n;
        while ((n = is.read(tmp)) != -1) buf.write(tmp, 0, n);
        is.close();
        return buf.toString("UTF-8");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (webView == null) return super.onKeyDown(keyCode, event);

        // Never exit on back — always send to JS
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            webView.evaluateJavascript(
                "document.dispatchEvent(new KeyboardEvent('keydown',{key:'Backspace',bubbles:true}))",
                null
            );
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
            webView.evaluateJavascript(
                "(function(){ var v=document.getElementById('vid'); if(v){ v.paused?v.play():v.pause(); } })()",
                null
            );
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP) {
            webView.evaluateJavascript(
                "(function(){ var b=document.getElementById('pbk'); if(b) b.click(); })()",
                null
            );
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) { webView.onResume(); webView.resumeTimers(); }
        hideSystemUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) { webView.onPause(); webView.pauseTimers(); }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            container.removeView(webView);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
