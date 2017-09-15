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

package pw.thedrhax.util;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;

import java.util.LinkedList;

import pw.thedrhax.mosmetro.activities.SafeViewActivity;

/**
 * This class is used to download files via DownloadManager
 * and automatically open them with Intent.ACTION_VIEW.
 *
 * WRITE_EXTERNAL_STORAGE permission is not required because we download
 * these files to internal storage and grant read permission to other apps.
 */

public class Downloader {
    private final static IntentFilter DOWNLOAD_COMPLETE =
            new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);

    public static final String TYPE_APK = "application/vnd.android.package-archive";

    private Context context;
    private DownloadManager dm;
    private LinkedList<DownloadResultReceiver> receivers;

    public Downloader(Context context) {
        this.context = context;
        this.dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        this.receivers = new LinkedList<>();
    }

    // Workaround for Android < 5.0
    // See commit 6e2b7b4e81fbf81f7b1e846c0436ea06cc82669f
    public static boolean isSupported() {
        return Build.VERSION.SDK_INT >= 21;
    }

    public void get(String url, String title, String description, String mimeType) {
        if (!isSupported()) {
            context.startActivity(
                    new Intent(context, SafeViewActivity.class).putExtra("data", url)
            );
            return;
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))
                .setTitle(title)
                .setDescription(description)
                .setMimeType(mimeType)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        DownloadResultReceiver receiver = new DownloadResultReceiver(dm.enqueue(request));
        context.registerReceiver(receiver, DOWNLOAD_COMPLETE);
        receivers.add(receiver);
    }

    public void stop() {
        for (DownloadResultReceiver receiver : receivers) {
            context.unregisterReceiver(receiver);
        }
        receivers.clear();
    }

    public class DownloadResultReceiver extends BroadcastReceiver {
        private long id = 0; // Expected download id

        public DownloadResultReceiver(long id) {
            this.id = id;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
            if (id != this.id) return;

            DownloadManager.Query query = new DownloadManager.Query().setFilterById(id);
            Cursor cursor = dm.query(query);
            if (cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                if (DownloadManager.STATUS_SUCCESSFUL == cursor.getInt(column)) {
                    Uri uri = dm.getUriForDownloadedFile(id);
                    String type = dm.getMimeTypeForDownloadedFile(id);

                    Intent install = new Intent(Intent.ACTION_VIEW)
                            .setDataAndType(uri, type)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    context.startActivity(install);
                }
            }

            receivers.remove(this);
            context.unregisterReceiver(this);
        }
    }
}
