package pw.thedrhax.mosmetro.services;

import android.app.Service;
import android.content.Intent;

import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;

import pw.thedrhax.mosmetro.authenticator.Authenticator;
import pw.thedrhax.util.Logger;

public class AuthService extends Service {
    private Logger logger = new Logger();
    private AuthTask task;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    /*
     * Binding interface
     */

    public class AuthBinder extends Binder implements Logger.Control {
        private AuthService service = AuthService.this;

        public void setCallback (Callback callback) {
            service.callback = callback;
        }

        @Override
        public Logger getLogger() {
            return service.logger;
        }

        @Override
        public void setLogger (Logger logger) {
            service.logger = logger;
        }

        public void start (Authenticator connection) {
            if ((task == null) || (AsyncTask.Status.FINISHED == task.getStatus()))
                task = new AuthTask(connection);

            if (task.getStatus() == AsyncTask.Status.PENDING)
                task.execute();
        }

        public void stop() {
            if (task.getStatus() == AsyncTask.Status.RUNNING)
                task.cancel(false);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new AuthBinder();
    }

    /*
     * Callback API
     */

    public interface Callback {
        void onPreExecute();
        void onPostExecute(int result);
        void onCancelled();
    }

    private Callback callback = new Callback() {
        @Override
        public void onPreExecute() {

        }

        @Override
        public void onPostExecute(int result) {

        }

        @Override
        public void onCancelled() {

        }
    };

    /*
     * Authenticator wrapped into AsyncTask
     */

    private class AuthTask extends AsyncTask<Void, Object, Integer> {
        private Logger local_logger;
        private Authenticator connection;

        public AuthTask (Authenticator connection) {
            local_logger = new Logger() {
                @Override
                public void log(LEVEL level, String message) {
                    super.log(level, message);
                    publishProgress(level, message);
                }
            };

            this.connection = connection;
            this.connection.setLogger(local_logger);
        }

        @Override
        protected Integer doInBackground (Void... params) {
            local_logger.date();
            int result = connection.start();
            local_logger.date();
            return result;
        }

        // Show log messages in the UI thread
        @Override
        protected void onProgressUpdate(Object... values) {
            logger.log((Logger.LEVEL) values[0], (String) values[1]);
        }

        @Override
        protected void onCancelled(Integer integer) {
            connection.stop();
            callback.onCancelled();
        }

        @Override
        protected void onPreExecute() {
            callback.onPreExecute();
        }

        @Override
        protected void onPostExecute(Integer result) {
            callback.onPostExecute(result);
        }
    }
}
