package com.nouh.iptv.tv;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

public class MainActivity extends Activity {

    private static final String TAG = "NOUHIPTVtv";
    private WebView webView;
    private FrameLayout container;

    @SuppressLint({"SetJavaScriptEnabled"})
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

            // Use a FrameLayout container so we can swap views safely
            container = new FrameLayout(this);
            setContentView(container);

            initWebView();

        } catch (Exception e) {
            Log.e(TAG, "onCreate crash: " + e.getMessage(), e);
        }
    }

    @SuppressLint({"SetJavaScriptEnabled"})
    private void initWebView() {
        try {
            // Destroy old WebView if exists
            if (webView != null) {
                container.removeView(webView);
                webView.destroy();
                webView = null;
            }

            WebView.setWebContentsDebuggingEnabled(false);
            webView = new WebView(this);
            webView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ));

            // ── WebView settings ──────────────────────────────
            WebSettings ws = webView.getSettings();
            ws.setJavaScriptEnabled(true);
            ws.setDomStorageEnabled(true);
            ws.setDatabaseEnabled(true);
            ws.setMediaPlaybackRequiresUserGesture(false);
            ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            ws.setAllowFileAccessFromFileURLs(true);
            ws.setAllowUniversalAccessFromFileURLs(true);
            ws.setAllowFileAccess(true);
            ws.setLoadWithOverviewMode(true);
            ws.setUseWideViewPort(true);
            ws.setCacheMode(WebSettings.LOAD_DEFAULT);
            ws.setBlockNetworkImage(false);
            ws.setLoadsImagesAutomatically(true);
            ws.setSupportZoom(false);
            ws.setBuiltInZoomControls(false);
            ws.setDisplayZoomControls(false);
            // TV desktop user-agent
            ws.setUserAgentString(
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36"
            );

            CookieManager cm = CookieManager.getInstance();
            cm.setAcceptCookie(true);
            cm.setAcceptThirdPartyCookies(webView, true);

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    // Keep all navigation inside WebView
                    return false;
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    view.evaluateJavascript(
                        "(function(){" +
                        "  var first = document.querySelector('.ni,.pc,.card,.chcard');" +
                        "  if(first){ first.tabIndex=0; first.focus(); }" +
                        "})()", null
                    );
                }

                @Override
                public void onReceivedError(WebView view, WebResourceRequest request,
                                            WebResourceError error) {
                    // Only log — don't crash
                    if (request.isForMainFrame()) {
                        Log.e(TAG, "WebView error on main frame: " + error.getDescription());
                    }
                }

                @Override
                public boolean onRenderProcessGone(WebView view,
                                                   RenderProcessGoneDetail detail) {
                    Log.e(TAG, "Render process gone — restarting WebView");
                    // Restart WebView instead of crashing
                    runOnUiThread(() -> initWebView());
                    return true; // We handled it — don't crash the app
                }
            });

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
                    container.addView(customView, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    ));
                    webView.setVisibility(View.GONE);
                }

                @Override
                public void onHideCustomView() {
                    if (customView != null) {
                        container.removeView(customView);
                        customView = null;
                    }
                    if (customViewCallback != null) {
                        customViewCallback.onCustomViewHidden();
                        customViewCallback = null;
                    }
                    webView.setVisibility(View.VISIBLE);
                }
            });

            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            container.addView(webView);
            webView.loadUrl("file:///android_asset/www/index.html");

        } catch (Exception e) {
            Log.e(TAG, "initWebView crash: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (webView == null) return super.onKeyDown(keyCode, event);

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            webView.evaluateJavascript(
                "document.dispatchEvent(new KeyboardEvent('keydown',{key:'Backspace',bubbles:true}))",
                null
            );
            return true; // Always consume — never exit on back
        }

        // Media keys
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
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();
        }
        hideSystemUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
            webView.pauseTimers();
        }
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
