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

package pw.thedrhax.mosmetro.httpclient;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import pw.thedrhax.mosmetro.authenticator.InterceptorTask;
import pw.thedrhax.util.Listener;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.Randomizer;
import pw.thedrhax.util.Util;

/**
 * Implementation of WebViewClient that ignores redirects in onPageFinished()
 * Inspired by https://stackoverflow.com/a/25547544
 */
public class InterceptedWebViewClient extends WebViewClient {
    private final Listener<String> currentUrl = new Listener<String>("") {
        @Override
        public void onChange(String new_value) {
            Logger.log(InterceptedWebViewClient.this, "Current URL | " + new_value);
        }
    };

    private final String key;
    private final String interceptorScript;
    private final List<InterceptorTask> interceptors = new LinkedList<>();

    private Context context;
    private Randomizer random;
    private WebView webview;
    private Client client = null;
    private String next_referer;
    private String referer;

    public InterceptedWebViewClient(Context context, Client client, WebView webview) {
        this.context = context;
        this.random = new Randomizer(context);
        this.webview = webview;

        key = random.string(25).toLowerCase();

        try {
            interceptorScript =
                    Util.readAsset(context, "xhook.min.js") +
                    Util.readAsset(context, "webview-proxy.js")
                            .replaceAll("INTERCEPT_KEY", key);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read assets");
        }

        setClient(client);
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void setup() {
        webview.setWebViewClient(this);
        webview.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Logger.log(InterceptedWebViewClient.this, "Chrome | " + consoleMessage.message());
                return super.onConsoleMessage(consoleMessage);
            }
        });

        clear(webview);

        WebSettings settings = webview.getSettings();
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setJavaScriptEnabled(true);
        settings.setUserAgentString(random.cached_useragent());
        settings.setDomStorageEnabled(true);
    }

    /**
     * Clear user data: Cookies, History, Cache
     * Source: https://stackoverflow.com/a/31950789
     */
    @SuppressWarnings("deprecation")
    public void clear(WebView webview) {
        webview.clearCache(true);
        webview.clearHistory();

        CookieManager manager = CookieManager.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            manager.removeAllCookies(null);
            manager.flush();
        } else {
            CookieSyncManager syncmanager = CookieSyncManager.createInstance(context);
            syncmanager.startSync();
            manager.removeAllCookie();
            manager.removeSessionCookie();
            syncmanager.stopSync();
            syncmanager.sync();
        }
    }

    public void onDestroy() {
        webview.stopLoading();

        // Avoid WebView leaks
        // Source: https://stackoverflow.com/a/48596543
        ((ViewGroup) webview.getParent()).removeView(webview);
        webview.removeAllViews();
        webview.destroy();
    }

    public InterceptedWebViewClient setClient(Client client) {
        if (this.client != null) {
            for (InterceptorTask task : interceptors) {
                this.client.interceptors.remove(task);
            }
        }

        this.client = client;
        client.interceptors.addAll(interceptors);
        return this;
    }

    public String getUrl() {
        return currentUrl.get();
    }

    private WebResourceResponse webresponse(@NonNull HttpResponse response) {
        if (response.isHtml() && !response.getUrl().isEmpty()) {
            Logger.log(this, response.toString());
        }

        if (response.isHtml()) {
            Document doc = response.getPageContent();

            doc.head().insertChildren(0, new LinkedList<Element>() {{
                Element xhook = doc.createElement("script");
                xhook.attr("src", "https://" + key + "/webview-proxy.js");
                add(xhook);
            }});
        }

        WebResourceResponse result = new WebResourceResponse(
                response.headers.getMimeType(),
                response.headers.getEncoding(),
                response.getInputStream()
        );

        if (Build.VERSION.SDK_INT >= 21) {
            result.setResponseHeaders(new HashMap<String, String>() {{
                for (String name : response.headers.keySet()) {
                    if (name.equalsIgnoreCase(Headers.CSP)) {
                        continue;
                    }

                    put(name, response.headers.getFirst(name));
                }
            }});

            if (!response.getReason().isEmpty()) {
                result.setStatusCodeAndReasonPhrase(
                        response.getResponseCode(),
                        response.getReason()
                );
            }
        }

        return result;
    }

    private HttpResponse getToPost(String url) throws IOException {
        if (url.matches("https?://[^/]+/" + key + "\\?.*")) {
            Uri uri = Uri.parse(url);
            url = uri.getQueryParameter("url");

            Headers headers = new Headers();

            try {
                JSONParser parser = new JSONParser();
                JSONObject rawHeaders = (JSONObject) parser.parse(uri.getQueryParameter("headers"));

                if (rawHeaders != null) {
                    for (Object key : rawHeaders.keySet()) {
                        headers.setHeader((String) key, (String) rawHeaders.get(key));
                    }
                }
            } catch (ParseException | ClassCastException ex) {
                Logger.log(this, Log.getStackTraceString(ex));
                Logger.log(this, "Unable to parse headers");
            }

            String type = "text/plain";

            if (headers.containsKey(Headers.CONTENT_TYPE)) {
                type = headers.getFirst(Headers.CONTENT_TYPE);
                headers.remove(Headers.CONTENT_TYPE);
            }

            String body = uri.getQueryParameter("body");

            Logger.log(this, "POST " + url);

            HttpRequest request = client.post(url, body, type);
            request.headers.putAll(headers);
            return request.execute();
        } else if (url.matches("^https?://" + key + "/webview-proxy\\.js$")) {
            return new HttpResponse(client.get(url), interceptorScript, "text/javascript");
        } else {
            Logger.log(this, "GET " + url);
            return client.get(url).execute();
        }
    }

    @Override
    public synchronized WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        WebResourceResponse result = new WebResourceResponse(
                "text/html",
                "utf-8",
                new ByteArrayInputStream("".getBytes())
        );

        if (referer != null) {
            client.headers.setHeader(Headers.REFERER, referer);
        }

        if ("about:blank".equals(url)) return null;

        try {
            result = webresponse(getToPost(url));
        } catch (UnknownHostException ex) {
            onReceivedError(view, ERROR_HOST_LOOKUP, ex.toString(), url);
            return result;
        } catch (IOException ex) {
            Logger.log(this, ex.toString());
        }

        // Apply scheduled referer update
        if (next_referer != null && next_referer.equals(url)) {
            Logger.log(this, "Referer | Scheduled: " + next_referer);
            referer = next_referer;
            next_referer = null;
        }

        // First request sets referer for others
        if (referer == null) {
            Logger.log(this, "Referer | First: " + url);
            referer = url;
        }

        return result;
    }

    @Nullable @Override @TargetApi(21)
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        if ("POST".equals(request.getMethod())) {
            Logger.log(this, "WARNING: Cannot intercept POST request to " + request.getUrl());
        }

        return super.shouldInterceptRequest(view, request);
    }

    @Override @TargetApi(21)
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return shouldOverrideUrlLoading(view, request.getUrl().toString());
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        next_referer = url; // Schedule referer update
        return false;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        Logger.log(this, "onPageStarted(" + url + ")");
        currentUrl.set(url);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        Logger.log(this, "onPageFinished(" + url + ")");
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        super.onReceivedError(view, errorCode, description, failingUrl);
        if (Build.VERSION.SDK_INT < 23) {
            Logger.log(this, "Error: " + description);
        }
    }

    @Override @TargetApi(23)
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        super.onReceivedError(view, request, error);
        Logger.log(this, "Error: " + error.getDescription());
    }
}
