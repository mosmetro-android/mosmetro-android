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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.widget.Toast;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import pw.thedrhax.mosmetro.BuildConfig;
import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.activities.SafeViewActivity;
import pw.thedrhax.mosmetro.httpclient.CachedRetriever;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.UUID;
import pw.thedrhax.util.Version;

public class UpdateChecker {
    // Info from the app
    private final Context context;
    private final DownloadManager dm;
    private final SharedPreferences settings;
    private final CachedRetriever retriever;

    // Updater state
    private boolean update_failed = false;
    private boolean check_ignored = false;
    private boolean force_check = false;
    private long last_download = 0;

    public UpdateChecker(Context context) {
        this.context = context;
        this.dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        this.settings = PreferenceManager.getDefaultSharedPreferences(context);
        this.retriever = new CachedRetriever(context);
    }

    private final BroadcastReceiver onComplete = new BroadcastReceiver() {
        @SuppressLint("SetWorldReadable")
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (id != last_download) return;

            Uri uri = dm.getUriForDownloadedFile(id);

            Intent install = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (Build.VERSION.SDK_INT >= 24) {
                install.setData(uri);
            } else {
                File apk = new File(context.getExternalFilesDir(null), "update.apk");
                if (apk.exists()) apk.setReadable(true, false);
                install.setDataAndType(uri, "application/vnd.android.package-archive");
            }

            try {
                context.startActivity(install);
                return;
            } catch (ActivityNotFoundException ex) {
                install.setAction(Intent.ACTION_VIEW);
            }

            try {
                context.startActivity(install);
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(context, context.getString(R.string.error,
                        context.getString(R.string.update_error)
                ), Toast.LENGTH_LONG).show();
            }
        }
    };

    public void init() {
        context.registerReceiver(onComplete,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        context.registerReceiver(onComplete,
                new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));
    }

    public void deinit() {
        context.unregisterReceiver(onComplete);
    }

    /**
     * Force check for updates even if current last version is marked 'ignored' by user.
     */
    public UpdateChecker ignore(boolean skip_ignored) {
        this.check_ignored = !skip_ignored; return this;
    }

    /**
     * Clear branch cache before checking for updates and force update notification even if
     * there are no updates available.
     */
    public UpdateChecker force(boolean force) {
        this.force_check = force; return this;
    }

    public Result check() {
        // Generate base URL
        String UPDATE_INFO_URL = settings.getString(
                BackendRequest.PREF_BACKEND_URL,
                BuildConfig.API_URL_DEFAULT
        ) + BuildConfig.API_REL_BRANCHES + "?uuid=" + UUID.get(context);

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

        Map<String, Branch> branches = new HashMap<>();
        Branch current_branch = null;

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

        return new Result(
            !update_failed && current_branch.hasUpdate(),
            current_branch,
            branches
        );
    }

    public void async_check(Callback callback) {
        new AsyncTask<Void,Void,Result>() {
            protected void onPreExecute() {
                callback.onStart();
            };

            @Override
            protected Result doInBackground(Void... aVoid) {
                return check();
            }

            @Override
            protected void onPostExecute(Result result) {
                callback.onResult(result);
            };
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public interface Callback {
        void onStart();
        void onResult(Result result);
    }

    public class Result {
        private boolean has_update;
        private Branch current_branch;
        private Map<String, Branch> branches;

        public Result(boolean has_update, Branch current_branch, Map<String, Branch> branches) {
            this.has_update = has_update;
            this.current_branch = current_branch;
            this.branches = branches;
        }

        public boolean hasUpdate() {
            return has_update;
        }

        public Branch getBranch() {
            return current_branch;
        }

        public Map<String, Branch> getBranches() {
            return branches;
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
    }

    public class Branch {
        public final String name;
        public final String message;
        public final String description;
        public final boolean stable;
        public final String url;
        public final String filename;

        private final int version;
        private final boolean by_build; // Check by build number instead of version code

        public Branch (String name, JSONObject data) {
            this.name = name;
            this.by_build = "1".equals(data.get("by_build"));
            this.version = Integer.parseInt((String)data.get(by_build ? "build" : "version"));
            this.message = ((String)data.get("message")).replace("<br>", "");
            this.description = (String)data.get("description");
            this.stable = data.containsKey("stable") && (Boolean)data.get("stable");
            this.url = (String)data.get("url");
            this.filename = (String)data.get("filename");
        }

        public String id() {
            return name + " " + version;
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

        public void install() {
            Uri uri = Uri.parse(settings.getString(
                    BackendRequest.PREF_BACKEND_URL,
                    BuildConfig.API_URL_DEFAULT
            ) + BuildConfig.API_REL_DOWNLOAD + "/" + name + "?uuid=" + UUID.get(context));

            File apk = new File(context.getExternalFilesDir(null), "update.apk");
            if (apk.exists()) {
                apk.delete();
            }

            DownloadManager.Request req = new DownloadManager.Request(uri)
                    .setTitle(filename)
                    .setDestinationUri(Uri.fromFile(apk))
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            last_download = dm.enqueue(req);
        }

        public void download() {
            context.startActivity(new Intent(context, SafeViewActivity.class).putExtra("data", url));
        }

        public AlertDialog.Builder dialog() {
            return new AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.update_available))
                    .setMessage(message)
                    .setNegativeButton(R.string.later, null)
                    .setNeutralButton(R.string.download, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            download();
                        }
                    })
                    .setPositiveButton(R.string.install, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            install();
                        }
                    });
        }

        public AlertDialog.Builder dialog_update() {
            return new AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.update_available))
                    .setMessage(message)
                    .setPositiveButton(R.string.install, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            install();
                        }
                    })
                    .setNeutralButton(R.string.download, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            download();
                        }
                    })
                    .setNegativeButton(R.string.ignore_short, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ignore(true);
                        }
                    });
        }
    }
}
