/**
 * Wi-Fi в метро (pw.thedrhax.mosmetro, Moscow Wi-Fi autologin)
 * Copyright © 2015 Dmitry Karikh <the.dr.hax@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pw.thedrhax.mosmetro.services;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.ParsedResponse;
import pw.thedrhax.mosmetro.httpclient.clients.OkHttp;
import pw.thedrhax.util.Listener;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.Randomizer;
import pw.thedrhax.util.Util;

public class WebViewService extends Service {
    private Listener<Boolean> running = new Listener<Boolean>(true) {
        @Override
        public void onChange(Boolean new_value) {
            if (!new_value) {
                stopSelf();
            }
        }
    };

    private String js_interface;
    private JavascriptListener js_result;

    private SharedPreferences settings;
    private ViewGroup view;
    private WindowManager wm;
    private WebView webview;

    @Override
    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    public void onCreate() {
        super.onCreate();
        setContentView(R.layout.webview_activity);
        webview = (WebView)view.findViewById(R.id.webview);

        Randomizer random = new Randomizer(this);

        js_interface = random.string("abcdef", 8);
        js_result = new JavascriptListener();
        webview.addJavascriptInterface(js_result, js_interface);

        clear();

        WebSettings settings = webview.getSettings();
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setJavaScriptEnabled(true);
        settings.setUserAgentString(random.cached_useragent());
        settings.setDomStorageEnabled(true);

        this.settings = PreferenceManager.getDefaultSharedPreferences(this);

        if (this.settings.getBoolean("pref_webview_debug", false)) {
            if (Build.VERSION.SDK_INT >= 19) {
                WebView.setWebContentsDebuggingEnabled(true);
            }
        }
    }

    /**
     * Clear user data: Cookies, History, Cache
     * Source: https://stackoverflow.com/a/31950789
     */
    @SuppressWarnings("deprecation")
    private void clear() {
        webview.clearCache(true);
        webview.clearHistory();

        CookieManager manager = CookieManager.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            manager.removeAllCookies(null);
            manager.flush();
        } else {
            CookieSyncManager syncmanager = CookieSyncManager.createInstance(this);
            syncmanager.startSync();
            manager.removeAllCookie();
            manager.removeSessionCookie();
            syncmanager.stopSync();
            syncmanager.sync();
        }
    }

    private void setContentView(@LayoutRes int layoutResID) {
        view = new LinearLayout(this);

        LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
        if (inflater != null) {
            inflater.inflate(layoutResID, view);
        } else {
            return;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.TYPE_TOAST,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
        );

        wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        if (wm != null) {
            wm.addView(view, params);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        webview.stopLoading();
        webview.removeJavascriptInterface(js_interface);

        // Avoid WebView leaks
        // Source: https://stackoverflow.com/a/48596543
        ((ViewGroup) webview.getParent()).removeView(webview);
        webview.removeAllViews();
        webview.destroy();
        webview = null;

        if (view != null && wm != null) {
            wm.removeView(view);
        }
        running.set(false);
    }

    // TODO: Return some information about the loaded page
    public void get(final String url) throws Exception {
        new Synchronizer<Boolean>() {
            @Override
            public void handlerThread() {
                webview.setWebViewClient(new FilteredWebViewClient() {
                    @Override
                    public void onPageCompletelyFinished(WebView view, String url) {
                        setResult(true);
                    }

                    @Override
                    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                        super.onReceivedError(view, request, error);
                        if (Build.VERSION.SDK_INT >= 23) {
                            Logger.log(WebViewService.this, (String) error.getDescription());
                        }
                    }

                    @Override
                    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                        super.onReceivedError(view, errorCode, description, failingUrl);
                        if (Build.VERSION.SDK_INT < 23) {
                            Logger.log(WebViewService.this, description);
                        }
                    }
                });
                webview.loadUrl(url);
            }
        }.run(webview.getHandler());
    }

    @Nullable
    public String js(final String script) throws Exception {
        return new Synchronizer<String>(1000) {
            @Override
            public void handlerThread() {
                new Listener<String>(null) {
                    @Override
                    public void onChange(String new_value) {
                        setResult(new_value);
                        unsubscribe(js_result);
                    }
                }.subscribe(js_result);
                webview.loadUrl("javascript:" + js_interface + ".onResult(String(" + script + "));");
            }
        }.run(webview.getHandler());
    }

    public void setCookies(String url, Map<String, String> cookies) {
        CookieManager manager = CookieManager.getInstance();

        CookieSyncManager syncmanager = null;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            syncmanager = CookieSyncManager.createInstance(this);
            syncmanager.startSync();
        }

        for (String name : cookies.keySet()) {
            manager.setCookie(url, name + "=" + cookies.get(name));
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            if (syncmanager != null) {
                syncmanager.stopSync();
                syncmanager.sync();
            }
        } else {
            manager.flush();
        }
    }

    public Map<String,String> getCookies(String url) {
        Map<String,String> result = new HashMap<>();

        String cookie_string = CookieManager.getInstance().getCookie(url);
        if (cookie_string != null) {
            String[] cookies = cookie_string.split("; ");
            for (String cookie : cookies) {
                String[] name_value = cookie.split("=");
                result.put(name_value[0], name_value.length > 1 ? name_value[1] : "");
            }
        }

        return result;
    }

    private class JavascriptListener extends Listener<String> {
        JavascriptListener() {
            super(null);
        }

        @JavascriptInterface
        public void onResult(String result) {
            set(result);
        }
    }

    public abstract class Synchronizer<T> {
        private Listener<T> result = new Listener<>(null);
        private Listener<String> error = new Listener<>(null);

        private final int timeout;

        public Synchronizer(int timeout) {
            this.timeout = timeout;
        }

        public Synchronizer() {
            this(60000);
        }

        /**
         * This method will be executed on Handler's thread.
         * It MUST call the setResult(T result) method! Call can be asynchronous.
         */
        public abstract void handlerThread();

        protected void setError(String message) {
            this.error.set(message);
        }

        protected void setResult(T result) {
            this.result.set(result);
        }

        @Nullable
        public T run(Handler handler) throws Exception {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    handlerThread();
                }
            });

            int counter = 0;
            while (result.get() == null) {
                if (counter >= timeout) {
                    throw new TimeoutException("Synchronizer timed out");
                }

                if (error.get() != null) {
                    throw new Exception(error.get());
                }

                if (!running.get()) {
                    throw new InterruptedException("Interrupted by Listener");
                }

                counter += 100;
                SystemClock.sleep(100);
            }

            return result.get();
        }
    }

    /**
     * Implementation of WebViewClient that ignores redirects in onPageFinished()
     * Inspired by https://stackoverflow.com/a/25547544
     *
     * TODO: Referer is not reliable. It has been tested only on JavaScript redirect
     */
    private abstract class FilteredWebViewClient extends WebViewClient {
        private boolean finished = true;
        private boolean redirecting = false;

        private Client client = new OkHttp(WebViewService.this).setRunningListener(running);
        private int pref_retry_count = Util.getIntPreference(WebViewService.this, "pref_retry_count", 3);

        private String next_referer;
        private String referer;

        @Override
        public synchronized WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            WebResourceResponse result = null;

            // Avoid crash during remote debugging
            if ("about:blank".equals(url)) {
                return null;
            }

            if (referer != null) {
                client.setHeader(Client.HEADER_REFERER, referer);
            }

            try {
                client.setCookies(url, getCookies(url));
                final ParsedResponse response = client.get(url, null, pref_retry_count);
                setCookies(url, client.getCookies(url));

                if (response != null) {
                    Logger.log(Logger.LEVEL.DEBUG, url);

                    if (response.getMimeType().contains("text/html")) {
                        Logger.log(Logger.LEVEL.DEBUG, response.toString());
                    }

                    result = new WebResourceResponse(
                            response.getMimeType(),
                            response.getEncoding(),
                            response.getInputStream()
                    );

                    if (Build.VERSION.SDK_INT >= 21) {
                        result.setResponseHeaders(new HashMap<String, String>() {{
                            Map<String,List<String>> headers = response.getHeaders();
                            for (String name : headers.keySet()) {
                                if (headers.get(name) != null && headers.get(name).size() == 1) {
                                    put(name, headers.get(name).get(0));
                                }
                            }

                            if (referer != null) {
                                Uri uri = Uri.parse(referer);
                                remove("access-control-allow-origin");
                                put("access-control-allow-origin", uri.getScheme() + "://" + uri.getHost());
                                put("access-control-allow-credentials", "true");
                            }
                            // remove("x-xss-protection");
                        }});
                        result.setStatusCodeAndReasonPhrase(
                                response.getResponseCode(),
                                response.getReason()
                        );
                    }
                }
            } catch (IOException ex) {
                Logger.log(Logger.LEVEL.DEBUG, ex);
            }

            // Apply scheduled referer update
            if (next_referer != null && next_referer.equals(url)) {
                Logger.log(WebViewService.this, "Referer | Scheduled: " + next_referer);
                referer = next_referer;
                next_referer = null;
            }

            // First request sets referer for others
            if (referer == null) {
                Logger.log(WebViewService.this, "Referer | First: " + url);
                referer = url;
            }

            return result;
        }

        @Override
        @TargetApi(24)
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return shouldOverrideUrlLoading(view, request.getUrl().toString());
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (!finished) {
                redirecting = true;
            } else {
                finished = false;
            }

            // Schedule referer update
            next_referer = url;

            view.loadUrl(url);
            return true;
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            if (settings.getBoolean("pref_trust_all_certs", false)) {
                handler.proceed();
            } else {
                super.onReceivedSslError(view, handler, error);
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            finished = false;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            if (!redirecting) {
                finished = true;
            }

            if (finished && !redirecting) {
                onPageCompletelyFinished(view, url);
            } else {
                redirecting = false;
            }
        }

        public abstract void onPageCompletelyFinished(WebView view, String url);
    }

    /*
     * Listener interface
     */

    public Listener<Boolean> getRunningListener() {
        return running;
    }

    /*
     * Binding interface
     */

    public class ScriptedWebViewBinder extends Binder {
        public WebViewService getService() {
            return WebViewService.this;
        }
    }

    private final IBinder binder = new ScriptedWebViewBinder();

    @Nullable @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
