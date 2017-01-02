/**
 * Wi-Fi в метро (pw.thedrhax.mosmetro, Moscow Wi-Fi autologin)
 * Copyright © 2015 Dmitry Karikh <the.dr.hax@gmail.com>
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pw.thedrhax.mosmetro.authenticator.providers;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.jsoup.nodes.Element;

import java.net.ProtocolException;
import java.util.HashMap;
import java.util.Map;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.authenticator.Provider;
import pw.thedrhax.mosmetro.authenticator.Task;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.clients.OkHttp;
import pw.thedrhax.util.Logger;

/**
 * The MosMetroV2 class supports the actual version of the MosMetro algorithm.
 *
 * Detection: Meta-redirect contains ".wi-fi.ru" with any 3rd level domain (except "login").
 *
 * @author Dmitry Karikh <the.dr.hax@gmail.com>
 * @see Provider
 */

public class MosMetroV2 extends Provider {
    private String redirect;

    public MosMetroV2(final Context context) {
        super(context);

        /**
         * Checking Internet connection for a first time
         * ⇒ GET http://wi-fi.ru
         * ⇐ Meta-redirect: http://auth.wi-fi.ru/?rand=... > redirect
         */
        add(new Task() {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                logger.log(context.getString(R.string.auth_checking_connection));

                if (isConnected()) {
                    logger.log(context.getString(R.string.auth_already_connected));
                    vars.put("result", RESULT.ALREADY_CONNECTED);
                    return false;
                } else {
                    return true;
                }
            }
        });

        /**
         * Getting redirect
         * ⇒ GET http://auth.wi-fi.ru/?rand=... < redirect
         * ⇐ JavaScript Redirect: http://auth.wi-fi.ru/auth
         */
        add(new Task() {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                logger.log(context.getString(R.string.auth_redirect));

                try {
                    client.get(redirect, null, pref_retry_count);
                    logger.log(Logger.LEVEL.DEBUG, client.getPageContent().outerHtml());
                    return true;
                } catch (Exception ex) {
                    logger.log(Logger.LEVEL.DEBUG, ex);
                    logger.log(String.format(
                            context.getString(R.string.error),
                            context.getString(R.string.auth_error_redirect)
                    ));
                    return false;
                }
            }
        });

        /**
         * Following JavaScript redirect to the auth page
         * redirect = "scheme://host"
         * ⇒ GET http://auth.wi-fi.ru/auth < redirect + "/auth"
         * ⇐ Form: method="post" action="/auto_auth" (captcha)
         * ⇐ AJAX: http://auth.wi-fi.ru/auth/init?segment=metro (no captcha)
         */
        add(new Task() {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                logger.log(context.getString(R.string.auth_auth_page));

                try {
                    Uri redirect_uri = Uri.parse(redirect);
                    redirect = redirect_uri.getScheme() + "://" + redirect_uri.getHost();

                    client.get(redirect + "/auth", null, pref_retry_count);
                    logger.log(Logger.LEVEL.DEBUG, client.getPageContent().outerHtml());
                    return true;
                } catch (Exception ex) {
                    logger.log(Logger.LEVEL.DEBUG, ex);
                    logger.log(String.format(
                            context.getString(R.string.error),
                            context.getString(R.string.auth_error_auth_page)
                    ));
                    return false;
                }
            }
        });

        /**
         * Handle CAPTCHA request
         */
        add(new Task() {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                Element form = client.getPageContent().getElementsByTag("form").first();
                if (form != null && "captcha__container".equals(form.attr("class"))) {
                    // Trying to bypass captcha using official backdoor
                    logger.log(context.getString(R.string.auth_captcha_bypass));
                    try {
                        int code = new OkHttp()
                                .resetHeaders()
                                .setHeader(
                                        new String(Base64.decode(
                                                "VXNlci1BZ2VudA==", Base64.DEFAULT
                                        )),
                                        new String(Base64.decode(
                                                "QXV0b01vc01ldHJvV2lmaS8xLjUuMCAo" +
                                                        "TGludXg7IEFuZHJvaWQgNC40LjQ7IEEw" +
                                                        "MTIzIEJ1aWxkL0tUVTg0UCk=", Base64.DEFAULT
                                        ))
                                )
                                .get(
                                        new String(Base64.decode(
                                                "aHR0cHM6Ly9hbW13LndpLWZpLnJ1L25l" +
                                                        "dGluZm8vYXV0aA==", Base64.DEFAULT
                                        )), null, pref_retry_count)
                                .getResponseCode();

                        if (code == 200 && isConnected()) {
                            logger.log(context.getString(R.string.auth_captcha_bypass_success));
                            vars.put("result", RESULT.CONNECTED);
                            return false;
                        } else {
                            throw new Exception("Internet check failed");
                        }
                    } catch (Exception ex) {
                        logger.log(Logger.LEVEL.DEBUG, ex);
                        logger.log(context.getString(R.string.auth_captcha_bypass_fail));
                    }

                    // Parsing captcha URL
                    String captcha_url;
                    try {
                        Element captcha_img = form.getElementsByTag("img").first();
                        captcha_url = redirect + captcha_img.attr("src");
                    } catch (Exception ex) {
                        logger.log(String.format(
                                context.getString(R.string.error),
                                context.getString(R.string.auth_error_captcha_image))
                        );
                        logger.log(Logger.LEVEL.DEBUG, ex);
                        vars.put("result", RESULT.CAPTCHA);
                        return false;
                    }

                    // Asking user to enter the code
                    String code = new CaptchaRunnable(captcha_url).getResult();
                    if (code.isEmpty()) {
                        logger.log(String.format(
                                context.getString(R.string.error),
                                context.getString(R.string.auth_error_captcha))
                        );
                        vars.put("result", RESULT.CAPTCHA);
                        return false;
                    }

                    logger.log(Logger.LEVEL.DEBUG, String.format(
                            context.getString(R.string.auth_captcha_result), code
                    ));

                    // Sending captcha form
                    logger.log(context.getString(R.string.auth_request));
                    Map<String,String> fields = Client.parseForm(form);
                    fields.put("_rucaptcha", code);

                    try {
                        client.post(redirect + form.attr("action"), fields, pref_retry_count);
                        logger.log(Logger.LEVEL.DEBUG, client.getPageContent().toString());
                    } catch (Exception ex) {
                        logger.log(Logger.LEVEL.DEBUG, ex);
                        logger.log(String.format(
                                context.getString(R.string.error),
                                context.getString(R.string.auth_error_server)
                        ));
                        return false;
                    }
                }
                return true;
            }
        });

        /**
         * Sending login form
         * ⇒ POST http://auth.wi-fi.ru/auth/init?... < redirect + "/auth/init?segment=metro"
         * ⇒ Cookie: afVideoPassed = 0
         * ⇒ Header: CSRF-Token = ...
         * ⇐ JSON
         */
        add(new Task() {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                logger.log(context.getString(R.string.auth_auth_form));

                try {
                    String csrf_token = client.parseMetaContent("csrf-token");
                    client.setHeader(Client.HEADER_CSRF, csrf_token);
                    client.setCookie(redirect, "afVideoPassed", "0");

                    client.post(redirect + "/auth/init?segment=metro", null, pref_retry_count);
                    logger.log(Logger.LEVEL.DEBUG, client.getPageContent().outerHtml());
                } catch (ProtocolException ignored) { // Too many follow-up requests
                } catch (Exception ex) {
                    logger.log(Logger.LEVEL.DEBUG, ex);
                    logger.log(String.format(
                            context.getString(R.string.error),
                            context.getString(R.string.auth_error_server)
                    ));
                    return false;
                }
                return true;
            }
        });

        /**
         * Checking Internet connection
         * JSON result > status
         */
        add(new Task() {
            @Override
            public boolean run(HashMap<String, Object> vars) {
                logger.log(context.getString(R.string.auth_checking_connection));

                try {
                    JSONObject response = (JSONObject)new JSONParser().parse(client.getPage());
                    boolean status = (Boolean)response.get("result");

                    if (status && isConnected()) {
                        logger.log(context.getString(R.string.auth_connected));
                        vars.put("result", RESULT.CONNECTED);
                        return true;
                    } else {
                        logger.log(String.format(
                                context.getString(R.string.error),
                                context.getString(R.string.auth_error_connection)
                        ));
                        return false;
                    }
                } catch (Exception ex) {
                    logger.log(Logger.LEVEL.DEBUG, ex);
                    return false;
                }
            }
        });
    }

    @Override
    public boolean isConnected() {
        Client client = new OkHttp().followRedirects(false);
        try {
            client.get("http://wi-fi.ru", null, pref_retry_count);
        } catch (Exception ex) {
            logger.log(Logger.LEVEL.DEBUG, ex);
            return false;
        }

        try {
            redirect = client.parseMetaRedirect();
            logger.log(Logger.LEVEL.DEBUG, client.getPageContent().outerHtml());
            logger.log(Logger.LEVEL.DEBUG, redirect);
        } catch (Exception ex) {
            // Redirect not found => connected
            return super.isConnected();
        }

        // Redirect found => not connected
        return false;
    }

    /**
     * Checks if current network is supported by this Provider implementation.
     * @param client    Client instance to get the information from. Provider.find()
     *                  will execute one request to be analyzed by this method.
     * @return          True if response matches this Provider implementation.
     */
    public static boolean match(Client client) {
        try {
            String redirect = client.parseMetaRedirect();
            return redirect.contains(".wi-fi.ru") && !redirect.contains("login.wi-fi.ru");
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * The CaptchaRunnable class is used to prompt user to enter the CAPTCHA.
     * This operation must be executed on the UI thread and only on the Activity Context.
     */
    private class CaptchaRunnable implements Runnable {
        /**
         * Thread lock marker.
         */
        private boolean locked;

        /**
         * Result value of the CAPTCHA.
         */
        private String result = "";

        /**
         * URL of the CAPTCHA to solve.
         */
        private String url;

        /**
         * Lock current thread and wait for user to solve the CAPTCHA.
         * @return Solved CAPTCHA value.
         */
        public String getResult() {
            // Dialog can be created only on the Activity context
            if (context instanceof Activity) {
                locked = true;

                ((Activity)context).runOnUiThread(this);

                while (locked && !stopped) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {}
                }
            }

            return result;
        }

        /**
         * Main constructor
         * @param url   URL of the CAPTCHA to solve
         */
        public CaptchaRunnable (String url) {
            this.url = url;
        }

        @Override
        public void run() {
            final Dialog dialog = new Dialog(context);
            dialog.setTitle(R.string.auth_captcha_dialog);
            dialog.setContentView(R.layout.captcha_dialog);

            final Button submit_button = (Button) dialog.findViewById(R.id.submit_button);

            final EditText text_captcha = (EditText) dialog.findViewById(R.id.text_captcha);
            text_captcha.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                    if (i == EditorInfo.IME_ACTION_DONE) {
                        submit_button.performClick();
                        return true;
                    }
                    return false;
                }
            });

            final ImageView image_captcha = (ImageView) dialog.findViewById(R.id.image_captcha);
            image_captcha.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new AsyncTask<Void,Void,Bitmap>() {
                        @Override
                        protected Bitmap doInBackground(Void... voids) {
                            try {
                                return BitmapFactory.decodeStream(client.getInputStream(url));
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                logger.log(Logger.LEVEL.DEBUG, ex);
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Bitmap bitmap) {
                            if (bitmap != null) image_captcha.setImageBitmap(bitmap);
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            });
            image_captcha.performClick();

            submit_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    result = text_captcha.getText().toString();
                    locked = false;
                    dialog.hide();
                }
            });

            dialog.setCanceledOnTouchOutside(true);
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    locked = false;
                }
            });

            dialog.show();
        }
    }
}
