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
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final String TAG = "NOUHIPTVtv";
    private static final int PORT = 8765;

    private WebView webView;
    private FrameLayout container;
    private ServerSocket serverSocket;
    private ExecutorService executor;

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

            // Start local HTTP server then load WebView
            executor = Executors.newCachedThreadPool();
            startLocalServer();

            // Give server 300ms to start then load
            container.postDelayed(() -> initWebView(), 300);

        } catch (Exception e) {
            Log.e(TAG, "onCreate: " + e.getMessage(), e);
        }
    }

    // ── Tiny local HTTP server ─────────────────────────────
    private void startLocalServer() {
        executor.execute(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                Log.d(TAG, "Local server started on port " + PORT);
                while (!serverSocket.isClosed()) {
                    try {
                        Socket client = serverSocket.accept();
                        executor.execute(() -> handleRequest(client));
                    } catch (IOException e) {
                        if (!serverSocket.isClosed()) Log.e(TAG, "Accept error: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Server error: " + e.getMessage(), e);
            }
        });
    }

    private void handleRequest(Socket client) {
        try {
            // Read request line
            StringBuilder req = new StringBuilder();
            int c;
            while ((c = client.getInputStream().read()) != -1) {
                req.append((char) c);
                if (req.toString().contains("\r\n\r\n")) break;
            }

            // Parse path from "GET /path HTTP/1.1"
            String reqStr = req.toString();
            String path = "/index.html";
            if (reqStr.startsWith("GET ")) {
                String[] parts = reqStr.split(" ");
                if (parts.length >= 2) {
                    path = parts[1];
                    if (path.equals("/")) path = "/index.html";
                }
            }

            // Strip leading slash for asset path
            String assetPath = "www" + path;
            String mimeType = getMimeType(path);

            OutputStream out = client.getOutputStream();
            try {
                InputStream asset = getAssets().open(assetPath);
                byte[] data = readAll(asset);
                asset.close();
                String header = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: " + mimeType + "\r\n" +
                    "Content-Length: " + data.length + "\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Connection: close\r\n\r\n";
                out.write(header.getBytes());
                out.write(data);
            } catch (IOException e) {
                String err = "HTTP/1.1 404 Not Found\r\nConnection: close\r\n\r\n";
                out.write(err.getBytes());
            }
            out.flush();
            client.close();
        } catch (Exception e) {
            Log.e(TAG, "Request error: " + e.getMessage());
            try { client.close(); } catch (IOException ignored) {}
        }
    }

    private byte[] readAll(InputStream is) throws IOException {
        java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
        byte[] tmp = new byte[8192];
        int n;
        while ((n = is.read(tmp)) != -1) buf.write(tmp, 0, n);
        return buf.toByteArray();
    }

    private String getMimeType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".js"))   return "application/javascript";
        if (path.endsWith(".css"))  return "text/css";
        if (path.endsWith(".png"))  return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".svg"))  return "image/svg+xml";
        if (path.endsWith(".json")) return "application/json";
        return "application/octet-stream";
    }

    // ── WebView ────────────────────────────────────────────
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

            WebSettings ws = webView.getSettings();
            ws.setJavaScriptEnabled(true);
            ws.setDomStorageEnabled(true);
            ws.setDatabaseEnabled(true);
            ws.setMediaPlaybackRequiresUserGesture(false);
            ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            ws.setAllowFileAccess(true);
            ws.setLoadWithOverviewMode(true);
            ws.setUseWideViewPort(true);
            ws.setCacheMode(WebSettings.LOAD_NO_CACHE);
            ws.setSupportZoom(false);
            ws.setBuiltInZoomControls(false);
            ws.setDisplayZoomControls(false);
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
                    view.evaluateJavascript(
                        "(function(){ var f=document.querySelector('.ni,.pc'); if(f){f.tabIndex=0;f.focus();} })()",
                        null
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

            // Load via localhost instead of file:// — much more stable
            webView.loadUrl("http://localhost:" + PORT + "/index.html");

        } catch (Exception e) {
            Log.e(TAG, "initWebView: " + e.getMessage(), e);
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
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
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
        if (webView != null) { container.removeView(webView); webView.destroy(); webView = null; }
        if (serverSocket != null) { try { serverSocket.close(); } catch (IOException ignored) {} }
        if (executor != null) executor.shutdownNow();
        super.onDestroy();
    }
}
