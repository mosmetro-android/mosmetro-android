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

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.IBinder;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.LinearLayout;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.InterceptedWebViewClient;
import pw.thedrhax.mosmetro.httpclient.clients.OkHttp;
import pw.thedrhax.util.Listener;

public class WebViewService extends Service {
    private final Listener<Boolean> running = new Listener<Boolean>(true) {
        @Override
        public void onChange(Boolean new_value) {
            if (!new_value) {
                stopSelf();
            }
        }
    };

    private ViewGroup view;
    private WindowManager wm;
    private WebView webview;
    private InterceptedWebViewClient webviewclient;

    @Override
    public void onCreate() {
        super.onCreate();
        setContentView(R.layout.webview_activity);
        Client client = new OkHttp(this).setRunningListener(running);
        webview = (WebView)view.findViewById(R.id.webview);
        webviewclient = new InterceptedWebViewClient(this, client);
        webviewclient.setup(webview);
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
        webviewclient.onDestroy(webview);
        webview = null;
        if (view != null && wm != null) {
            wm.removeView(view);
        }
        destroyed.set(true);
    }

    private final Listener<Boolean> destroyed = new Listener<Boolean>(false);

    /**
     * @return Read-only Listener that will be updated as soon as service is
     *         destroyed.
     */
    public Listener<Boolean> onDestroyListener() {
        Listener<Boolean> result = new Listener<Boolean>(false);
        result.subscribe(destroyed);
        return result;
    }

    public void get(final String url) {
        webview.getHandler().post(new Runnable() {
            @Override
            public void run() {
                webview.loadUrl(url);
            }
        });
    }

    public String getUrl() {
        return webviewclient.getUrl();
    }

    public void setClient(Client client) {
        webviewclient.setClient(client);
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

    public class WebViewBinder extends Binder {
        public WebViewService getService() {
            return WebViewService.this;
        }
    }

    private final IBinder binder = new WebViewBinder();

    @Nullable @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        running.unsubscribe();
        running.set(false);
        return false;
    }
}
