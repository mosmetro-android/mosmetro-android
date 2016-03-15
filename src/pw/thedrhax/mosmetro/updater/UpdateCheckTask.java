package pw.thedrhax.mosmetro.updater;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.httpclient.CachedRetriever;

public abstract class UpdateCheckTask extends AsyncTask<Void,Void,Void> {
    private String UPDATE_INFO_URL;

    // Info from the app
    private final Context context;
    private final SharedPreferences settings;

    // Info from the server
    private Branch current_branch;

    // Updater state
    private boolean update_failed = false;
    private boolean check_ignored = false;

    public UpdateCheckTask (Context context) {
        this.context = context;
        this.settings = PreferenceManager.getDefaultSharedPreferences(context);
    }

    private boolean hasUpdate() {
        return !update_failed && current_branch.hasUpdate();
    }

    @Override
    protected Void doInBackground (Void... params) {
        CachedRetriever retriever = new CachedRetriever(context);

        UPDATE_INFO_URL = retriever
                .get(CachedRetriever.BASE_URL_SOURCE, "http://wi-fi.metro-it.com")  + "/update.php";

        // Retrieve info from server
        String content;
        try {
            content = retriever.get(UPDATE_INFO_URL, 60*60, "");

            if (content == null || content.isEmpty())
                throw new Exception ("Failed to receive info from the update server");
        } catch (Exception ex) {
            update_failed = true;
            return null;
        }

        // Parse server answer
        Document document = Jsoup.parse(content);
        for (Element element : document.getElementsByTag("branch")) {
            Branch branch = new Branch(element);
            if (branch.name.equals(settings.getString("pref_updater_branch", "play"))) {
                current_branch = branch;
                break;
            }
        }

        if (current_branch == null) {
            update_failed = true;
            return null;
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        result(hasUpdate(), current_branch);
    }

    public void showDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);

        if (hasUpdate()) {
            dialog = dialog
                    .setTitle(context.getString(R.string.updater_available))
                    .setMessage(current_branch.message)
                    .setNegativeButton(R.string.updater_ignore, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            current_branch.ignore(true);
                        }
                    })
                    .setNeutralButton(R.string.updater_later, null)
                    .setPositiveButton(R.string.install, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            current_branch.download();
                        }
                    });
        } else {
            dialog = dialog
                    .setTitle(context.getString(R.string.updater_not_available))
                    .setMessage(context.getString(R.string.updater_not_available_message))
                    .setNegativeButton(context.getString(R.string.ok), null);
        }

        try {
            dialog.show();
        } catch (Exception ignored) {}
    }

    public UpdateCheckTask setIgnore (boolean ignore) {
        check_ignored = !ignore; return this;
    }

    abstract public void result (boolean hasUpdate, Branch current_branch);

    public class Branch {
        public String name;
        public String message;

        private int version;
        private boolean by_build = false; // Check by build number instead of version code

        public Branch (Element element) {
            name = element.attr("id");

            int version = 0, build = 0;

            for (Element key : element.getElementsByTag("key")) {
                if (key.attr("id").equals("version"))
                    version = Integer.parseInt(key.html());

                if (key.attr("id").equals("build"))
                    build = Integer.parseInt(key.html());

                if (key.attr("id").equals("by_build") &&
                        key.html().equals("1"))
                    by_build = true;

                if (key.attr("id").equals("message"))
                    message = key.html().replace("<br>", "");
            }

            this.version = by_build ? build : version;
        }

        private int getVersion() {
            if (by_build) {
                return settings.getInt("pref_updater_build", 0);
            } else {
                try {
                    PackageInfo pInfo = context
                            .getPackageManager().getPackageInfo(context.getPackageName(), 0);

                    return pInfo.versionCode;
                } catch (PackageManager.NameNotFoundException ex) {
                    return Integer.MAX_VALUE;
                }
            }
        }

        public boolean hasUpdate() {
            if (settings.getInt("pref_updater_ignore", 0) < version || check_ignored)
                if (getVersion() < version) return true;

            return false;
        }

        public void ignore(boolean ignore) {
            settings.edit()
                    .putInt("pref_updater_ignore", ignore ? version : 0)
                    .apply();
        }

        public void download() {
            settings.edit()
                    .putInt("pref_updater_build", version)
                    .apply();

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(UPDATE_INFO_URL + "?download=" + name));
            context.startActivity(intent);
        }
    }
}
