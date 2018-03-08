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
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.ValueCallback;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import java.util.concurrent.TimeoutException;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.util.Listener;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.Randomizer;
import pw.thedrhax.util.Util;

public class WebViewService extends Service {
    private Listener<Boolean> running = new Listener<>(true);

    private ViewGroup view;
    private WindowManager wm;
    private WebView webview;

    private int pref_timeout;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    public void onCreate() {
        super.onCreate();
        setContentView(R.layout.webview_activity);
        webview = (WebView)view.findViewById(R.id.webview);

        clear();

        WebSettings settings = webview.getSettings();
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setJavaScriptEnabled(true);
        settings.setUserAgentString(new Randomizer(this).cached_useragent());

        pref_timeout = Util.getIntPreference(this, "pref_timeout", 5);
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

    @Nullable @RequiresApi(19)
    public String js(final String script) throws Exception {
        return new Synchronizer<String>() {
            @Override
            public void handlerThread() {
                webview.evaluateJavascript(script, new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        setResult(value);
                    }
                });
            }
        }.run(webview.getHandler());
    }

    public abstract class Synchronizer<T> {
        private Listener<T> result = new Listener<>(null);
        private Listener<String> error = new Listener<>(null);

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
                if (pref_timeout != 0 && counter++ >= pref_timeout * 10) {
                    throw new TimeoutException("Synchronizer timed out");
                }

                if (error.get() != null) {
                    throw new Exception(error.get());
                }

                if (!running.get()) {
                    throw new InterruptedException("Interrupted by Listener");
                }

                SystemClock.sleep(100);
            }

            return result.get();
        }
    }

    /**
     * Implementation of WebViewClient that ignores redirects in onPageFinished()
     * Inspired by https://stackoverflow.com/a/25547544
     */
    private abstract class FilteredWebViewClient extends WebViewClient {
        private boolean finished = true;
        private boolean redirecting = false;

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            Logger.log(WebViewService.this, "Request | " + url);
            return super.shouldInterceptRequest(view, url);
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
            view.loadUrl(url);
            return true;
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
