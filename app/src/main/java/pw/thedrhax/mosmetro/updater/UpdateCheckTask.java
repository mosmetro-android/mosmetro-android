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

package pw.thedrhax.mosmetro.updater;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.HashMap;
import java.util.Map;

import pw.thedrhax.mosmetro.BuildConfig;
import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.activities.SafeViewActivity;
import pw.thedrhax.mosmetro.httpclient.CachedRetriever;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.Version;

public class UpdateCheckTask extends AsyncTask<Void,Void,Void> {
    // Info from the app
    private final Context context;
    private final SharedPreferences settings;
    private final CachedRetriever retriever;

    // Info from the server
    private Map<String, Branch> branches;
    private Branch current_branch;

    // Updater state
    private boolean update_failed = false;
    private boolean check_ignored = false;
    private boolean force_check = false;

    public UpdateCheckTask (Context context) {
        this.context = context;
        this.settings = PreferenceManager.getDefaultSharedPreferences(context);
        this.retriever = new CachedRetriever(context);
    }

    /**
     * Force check for updates even if current last version is marked 'ignored' by user.
     */
    public UpdateCheckTask ignore(boolean skip_ignored) {
        this.check_ignored = !skip_ignored; return this;
    }

    /**
     * Clear branch cache before checking for updates and force update notification even if
     * there are no updates available.
     */
    public UpdateCheckTask force(boolean force) {
        this.force_check = force; return this;
    }

    @Override
    protected Void doInBackground (Void... aVoid) {
        // Generate base URL
        String UPDATE_INFO_URL = retriever.get(
                BuildConfig.API_URL_SOURCE, BuildConfig.API_URL_DEFAULT, CachedRetriever.Type.URL
        ) + BuildConfig.API_REL_BRANCHES;

        // Clear branch cache
        if (force_check) retriever.remove(UPDATE_INFO_URL);

        // Retrieve info from server
        String content = retriever.get(UPDATE_INFO_URL, 60*60,
                "{\"" + Version.getBranch() + "\":" +
                "{\"url\":\"none\",\"by_build\":\"0\",\"version\":\"" + Version.getVersionCode() +
                "\",\"message\":\"none\",\"description\":\"Connection error\",\"stable\":true}}",
                CachedRetriever.Type.JSON
        );

        // Parse server answer
        JSONObject branches_json;
        try {
            branches_json = (JSONObject) new JSONParser().parse(content);
        } catch (ParseException ex) {
            update_failed = true;
            return null;
        }

        branches = new HashMap<>();
        for (Object key : branches_json.keySet()) {
            Branch branch;

            try {
                branch = new Branch((String) key, (JSONObject) branches_json.get(key));
            } catch (NumberFormatException ex) {
                Logger.log(Logger.LEVEL.DEBUG, ex);
                continue;
            }

            if (Version.getBranch().equals(branch.name)) {
                current_branch = branch;
            }

            branches.put(branch.name, branch);
        }

        if (branches.size() == 0) {
            update_failed = true;
            return null;
        }

        // Check if selected branch is deleted
        if (current_branch == null && !Version.getBranch().startsWith("_")) { // Fallback to master
            settings.edit().putInt("pref_updater_ignore", 0).apply();

            if (branches.containsKey("master")) {
                current_branch = branches.get("master");
                current_branch.ignore(false);
            } else {
                update_failed = true;
                return null;
            }
        }

        if (current_branch == null) {
            update_failed = true;
            return null;
        }

        return null;
    }

    private boolean hasUpdate() {
        return !update_failed && current_branch.hasUpdate();
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        result(hasUpdate(), current_branch);
        result(branches);
    }

    public void showDialog() {
        AlertDialog.Builder dialog;

        if (hasUpdate()) {
            dialog = current_branch.dialog_update();
        } else {
            dialog = new AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.update_not_available))
                    .setMessage(context.getString(R.string.update_not_available_message))
                    .setNegativeButton(context.getString(R.string.ok), null);
        }

        try {
            dialog.show();
        } catch (Exception ignored) {}
    }

    public void result(boolean hasUpdate, @Nullable Branch current_branch) {
        if (hasUpdate || force_check) showDialog();
    }

    public void result(@Nullable Map<String, Branch> branches) {}

    public class Branch {
        public final String name;
        public final String message;
        public final String description;
        public final boolean stable;
        public final String url;

        private int version;
        private boolean by_build = false; // Check by build number instead of version code

        public Branch (String name, JSONObject data) {
            this.name = name;
            this.by_build = "1".equals(data.get("by_build"));
            this.version = Integer.parseInt((String)data.get(by_build ? "build" : "version"));
            this.message = ((String)data.get("message")).replace("<br>", "");
            this.description = (String)data.get("description");
            this.stable = data.containsKey("stable") && (Boolean)data.get("stable");
            this.url = (String)data.get("url");
        }

        private int getVersion() {
            if (by_build) {
                if (Version.getBranch().equals(name)) {
                    return Version.getBuildNumber();
                } else {
                    return 0;
                }
            } else {
                return Version.getVersionCode();
            }
        }

        public boolean hasUpdate() {
            if (settings.getInt("pref_updater_ignore", 0) < version || check_ignored)
                if (getVersion() < version) return true;

            return false;
        }

        public void ignore(boolean ignore) {
            settings.edit().putInt("pref_updater_ignore", ignore ? version : 0).apply();
        }

        public void download() {
            context.startActivity(new Intent(context, SafeViewActivity.class).putExtra("data", url));
        }

        public AlertDialog.Builder dialog() {
            return new AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.update_available))
                    .setMessage(message)
                    .setNeutralButton(R.string.later, null)
                    .setPositiveButton(R.string.install, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            download();
                        }
                    });
        }

        public AlertDialog.Builder dialog_update() {
            return dialog()
                    .setNegativeButton(R.string.ignore_short, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ignore(true);
                        }
                    });
        }
    }
}
