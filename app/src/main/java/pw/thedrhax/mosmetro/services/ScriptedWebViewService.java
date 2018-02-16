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
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.Randomizer;

public class ScriptedWebViewService extends Service {
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_SCRIPT = "script";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_RESULT = "result";
    public static final String EXTRA_COOKIES = "cookies";
    public static final String EXTRA_CALLBACK = "callback";

    public static final String RESULT_SUCCESS = "SUCCESS";
    public static final String RESULT_ERROR = "ERROR";

    private ViewGroup view;
    private WindowManager wm;
    private WebView webview;

    @Override
    public void onCreate() {
        super.onCreate();
        setContentView(R.layout.webview_activity);
        webview = view.findViewById(R.id.webview);
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
                300,300, 0, 0,
                WindowManager.LayoutParams.TYPE_TOAST,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        if (wm != null) {
            wm.addView(view, params);
        }
    }

    private void callback(Intent intent, String result) {
        if (intent != null && intent.hasExtra(EXTRA_CALLBACK)) {
            Intent callback = new Intent(intent.getStringExtra(EXTRA_CALLBACK));

            String cookie_string = CookieManager.getInstance()
                    .getCookie(intent.getStringExtra(EXTRA_URL));
            if (cookie_string != null) {
                String[] cookies = cookie_string.split("; ");
                callback.putExtra(EXTRA_COOKIES, cookies);
            }

            if (result != null)
                callback.putExtra(EXTRA_RESULT, result);

            sendBroadcast(callback);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @RequiresApi(19)
    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        if (intent == null || !intent.hasExtra(EXTRA_URL) || !intent.hasExtra(EXTRA_SCRIPT)) {
            stopSelf(); return START_NOT_STICKY;
        }

        TextView text = view.findViewById(R.id.text);
        text.setText(intent.getStringExtra(EXTRA_MESSAGE));

        final ValueCallback<String> vc = new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                Logger.log(ScriptedWebViewService.this, "Received value: " + value);

                if ("\"SUCCESS\"".equals(value)) {
                    callback(intent, RESULT_SUCCESS);
                }

                if ("\"ERROR\"".equals(value)) {
                    callback(intent, RESULT_ERROR);
                }
            }
        };

        webview.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Logger.log(webview, "onPageFinished | " + url);
                view.evaluateJavascript(intent.getStringExtra(EXTRA_SCRIPT), vc);
            }
        });

        WebSettings settings = webview.getSettings();
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setJavaScriptEnabled(true);
        settings.setUserAgentString(new Randomizer(this).cached_useragent());

        webview.loadUrl(intent.getStringExtra(EXTRA_URL));

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (view != null && wm != null) {
            wm.removeView(view);
        }
    }

    @Nullable @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
