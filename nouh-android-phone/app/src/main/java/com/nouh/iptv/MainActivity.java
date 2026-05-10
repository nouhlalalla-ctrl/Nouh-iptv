package com.nouh.iptv;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {

    private WebView webView;

    @SuppressLint({"SetJavaScriptEnabled", "WebViewApiAvailability"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full-screen immersive
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemUI();

        webView = new WebView(this);
        setContentView(webView);

        // ── WebView settings ──────────────────────────────────
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);          // localStorage for profile storage
        ws.setDatabaseEnabled(true);
        ws.setMediaPlaybackRequiresUserGesture(false);  // auto-play streams
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW); // http streams on https pages
        ws.setAllowFileAccessFromFileURLs(true);
        ws.setAllowUniversalAccessFromFileURLs(true);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setCacheMode(WebSettings.LOAD_DEFAULT);
        ws.setUserAgentString(
            "Mozilla/5.0 (Linux; Android 11; Pixel 4) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36"
        );

        // Accept all cookies (needed for Stalker portal auth)
        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(webView, true);

        // ── WebViewClient — intercept requests to add CORS bypass headers ──
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                // Let all requests through — CORS is handled by network_security_config
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Keep all navigation inside the WebView
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Inject viewport and screen-wake helper after page loads
                view.evaluateJavascript(
                    "(function(){ " +
                    "  document.documentElement.style.setProperty('--safe-bottom', '0px');" +
                    "  if(window.screen && window.screen.orientation){" +
                    "    window.screen.orientation.unlock && window.screen.orientation.unlock();" +
                    "  }" +
                    "})()", null
                );
            }
        });

        // Allow video fullscreen via WebChromeClient
        webView.setWebChromeClient(new WebChromeClient() {
            private View customView;
            private CustomViewCallback customViewCallback;

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                setContentView(customView);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }

            @Override
            public void onHideCustomView() {
                setContentView(webView);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                if (customView != null) {
                    customViewCallback.onCustomViewHidden();
                    customView = null;
                }
            }
        });

        // Enable hardware acceleration on WebView
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // Load the app
        webView.loadUrl("file:///android_asset/www/index.html");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Back button — go back in WebView history or exit
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        webView.resumeTimers();
        hideSystemUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
        webView.pauseTimers();
    }

    @Override
    protected void onDestroy() {
        webView.destroy();
        super.onDestroy();
    }
}
