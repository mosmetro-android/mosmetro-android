package pw.thedrhax.mosmetro.authenticator;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.SystemClock;

import pw.thedrhax.mosmetro.services.WebViewService;

/**
 * Base class for all WebView-specific providers.
 *
 * @author Dmitry Karikh <the.dr.hax@gmail.com>
 * @see Provider
 * @see Task
 */

public abstract class WebViewProvider extends Provider {

    public WebViewProvider(Context context) {
        super(context);
    }

    @Override
    public RESULT start() {
        Intent intent = new Intent(
                context, WebViewService.class
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.bindService(intent, connection, Context.BIND_AUTO_CREATE);

        while (wv == null) {
            SystemClock.sleep(100);

            if (!running.get()) {
                stop();
                return RESULT.INTERRUPTED;
            }
        }

        RESULT result = super.start();

        stop();

        return result;
    }

    public void stop() {
        context.unbindService(connection);
    }

    /*
     * Binding interface
     */

    protected WebViewService wv = null;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            wv = ((WebViewService.ScriptedWebViewBinder)iBinder).getService();
            wv.getRunningListener().subscribe(running);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if (wv != null) {
                wv.getRunningListener().unsubscribe(running);
                wv = null;
            }
        }
    };
}
