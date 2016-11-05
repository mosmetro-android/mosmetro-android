package pw.thedrhax.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class Version {
    private PackageInfo pInfo = null;

    public Version (Context context) {
        try {
            pInfo = context
                    .getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException ignored) {}
    }

    public String getVersionName() {
        return (pInfo != null) ? pInfo.versionName : "unknown";
    }

    public int getVersionCode() {
        return (pInfo != null) ? pInfo.versionCode : Integer.MAX_VALUE;
    }

    public String getFormattedVersion() {
        return getVersionName() + "-" + getVersionCode();
    }
}
