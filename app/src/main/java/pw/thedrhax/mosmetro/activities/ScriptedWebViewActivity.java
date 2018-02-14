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

package pw.thedrhax.mosmetro.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.Randomizer;

public class ScriptedWebViewActivity extends Activity {
    public static final String ACTION_FINISH = "pw.thedrhax.activity.ScriptedWebViewActivity.FINISH";

    public static final String EXTRA_URL = "url";
    public static final String EXTRA_SCRIPT = "script";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_RESULT = "result";
    public static final String EXTRA_COOKIES = "cookies";
    public static final String EXTRA_CALLBACK = "callback";

    public static final String RESULT_SUCCESS = "SUCCESS";
    public static final String RESULT_ERROR = "ERROR";

    private WebView webview;

    private BroadcastReceiver receiver;

    private String result = null;

    @SuppressLint("SetJavaScriptEnabled")
    @RequiresApi(19)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webview_activity);

        final Intent intent = getIntent();
        if (intent == null || !intent.hasExtra(EXTRA_URL) || !intent.hasExtra(EXTRA_SCRIPT)) {
            finish(); return;
        }

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                finish();
            }
        };
        registerReceiver(receiver, new IntentFilter(ACTION_FINISH));

        TextView text = findViewById(R.id.text);
        text.setText(intent.getStringExtra(EXTRA_MESSAGE));

        final ValueCallback<String> vc = new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                Logger.log(ScriptedWebViewActivity.this, "Received value: " + value);

                if ("\"SUCCESS\"".equals(value)) {
                    result = RESULT_SUCCESS; finish();
                }

                if ("\"ERROR\"".equals(value)) {
                    result = RESULT_ERROR; finish();
                }
            }
        };

        webview = findViewById(R.id.webview);
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_CALLBACK)) {
            Intent callback = new Intent(intent.getStringExtra(EXTRA_CALLBACK));

            String[] cookies = CookieManager.getInstance()
                    .getCookie(intent.getStringExtra(EXTRA_URL)).split("; ");
            callback.putExtra(EXTRA_COOKIES, cookies);

            if (result != null)
                callback.putExtra(EXTRA_RESULT, result);

            sendBroadcast(callback);
        }
    }
}
