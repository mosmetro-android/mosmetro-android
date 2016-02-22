package pw.thedrhax.mosmetro.updater;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import pw.thedrhax.httpclient.HttpClient;

import java.util.LinkedList;
import java.util.List;

public abstract class UpdateCheckTask extends AsyncTask<Void,Void,Void> {
    private static final String UPDATE_BASE_URL = "http://thedrhax.pw/mosmetro/";
    private static final String UPDATE_INFO_URL = UPDATE_BASE_URL + "update.php";

    // Info from the app
    private final Context context;
    private final SharedPreferences settings;

    // Info from the server
    private List<Branch> branches;
    private Branch current_branch;

    // Updater state
    private boolean update_failed = false;

    public UpdateCheckTask (Context context) {
        this.context = context;
        this.settings = PreferenceManager.getDefaultSharedPreferences(context);
    }

    private int getVersionCode() {
        try {
            PackageInfo pInfo = context
                    .getPackageManager().getPackageInfo(context.getPackageName(), 0);

            return pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException ex) {
            return Integer.MAX_VALUE;
        }
    }

    private boolean hasUpdate() {
        return !update_failed && getVersionCode() < current_branch.version;
    }

    @Override
    protected Void doInBackground (Void... params) {
        HttpClient client = new HttpClient().setMaxRetries(3);

        // Retrieve info from server
        String content;
        try {
            content = client.navigate(UPDATE_INFO_URL).getContent();
            if (content == null || content.isEmpty())
                throw new Exception ("Failed to receive info from the update server");
        } catch (Exception ex) {
            update_failed = true;
            return null;
        }

        // Parse server answer
        Document document = Jsoup.parse(content);
        branches = new LinkedList<Branch>();
        for (Element branch : document.getElementsByTag("branch")) {
            branches.add(new Branch(branch));
        }

        if (branches.isEmpty()) {
            update_failed = true;
            return null;
        }

        // Search for current branch in parsed info
        for (Branch branch : branches) {
            if (branch.name.equals(settings.getString("pref_updater_branch", "play")))
                current_branch = branch;
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

    abstract public void result (boolean hasUpdate, Branch current_branch);

    public class Branch {
        public String name;
        public int version;
        public int build;
        public String message;

        public Branch (Element element) {
            name = element.attr("id");

            for (Element key : element.getElementsByTag("key")) {
                if (key.attr("id").equals("version"))
                    version = Integer.parseInt(key.html());

                if (key.attr("id").equals("build"))
                    build = Integer.parseInt(key.html());

                if (key.attr("id").equals("message"))
                    message = key.html().replace("<br>", "");
            }
        }

        public String getDownloadUrl () {
            return UPDATE_BASE_URL + "releases/MosMetro-" + name + "-v" + version + "-b" + build + ".apk";
        }
    }
}
