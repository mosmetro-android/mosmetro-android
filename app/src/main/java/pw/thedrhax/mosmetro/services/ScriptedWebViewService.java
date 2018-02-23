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
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.util.Listener;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.Randomizer;

public class ScriptedWebViewService extends Service {
    private Listener<Boolean> running = new Listener<>(true);

    private ViewGroup view;
    private WindowManager wm;
    private WebView webview;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    public void onCreate() {
        super.onCreate();
        setContentView(R.layout.webview_activity);
        webview = (WebView)view.findViewById(R.id.webview);

        WebSettings settings = webview.getSettings();
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setJavaScriptEnabled(true);
        settings.setUserAgentString(new Randomizer(this).cached_useragent());
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
        if (view != null && wm != null) {
            wm.removeView(view);
        }
        running.set(false);
    }

    public boolean get(final String url) {
        return new Synchronizer<Boolean>() {
            @Override
            public void handlerThread() {
                webview.setWebViewClient(new FilteredWebViewClient() {
                    @Override
                    public void onPageCompletelyFinished(WebView view, String url) {
                        setResult(true);
                    }
                });
                webview.loadUrl(url);
            }
        }.run(webview.getHandler());
    }

    @Nullable @RequiresApi(19)
    public String js(final String script) {
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
        private Listener<T> listener = new Listener<>(null);

        /**
         * This method will be executed on Handler's thread.
         * It MUST call the setResult(T result) method! Call can be asynchronous.
         * TODO: Add timeout handlers
         */
        public abstract void handlerThread();

        protected void setResult(T result) {
            listener.set(result);
        }

        public T run(Handler handler) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    handlerThread();
                }
            });

            while (listener.get() == null) {
                SystemClock.sleep(100);

                if (!running.get()) {
                    return null;
                }
            }

            return listener.get();
        }
    }

    /**
     * Implementation of WebViewClient that ignores redirects in onPageFinished()
     * Inspired by https://stackoverflow.com/a/25547544
     * TODO: Add error handlers
     */
    private abstract class FilteredWebViewClient extends WebViewClient {
        private boolean finished = true;
        private boolean redirecting = false;

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            Logger.log(ScriptedWebViewService.this, "Request | " + url);
            return super.shouldInterceptRequest(view, url);
        }

        @Override
        @TargetApi(24)
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return super.shouldOverrideUrlLoading(view, request.getUrl().toString());
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
        public ScriptedWebViewService getService() {
            return ScriptedWebViewService.this;
        }
    }

    private final IBinder binder = new ScriptedWebViewBinder();

    @Nullable @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
