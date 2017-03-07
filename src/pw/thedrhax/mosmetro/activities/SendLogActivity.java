package pw.thedrhax.mosmetro.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.PermissionUtils;
import pw.thedrhax.util.Version;

public class SendLogActivity extends Activity {
    private Intent email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ongoing_dialog);


        email = new Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_EMAIL,
                        new String[] {getString(R.string.report_email_address)}
                )
                .putExtra(Intent.EXTRA_SUBJECT,
                        getString(R.string.report_email_subject, Version.getFormattedVersion())
                );

        PermissionUtils pu = new PermissionUtils(this);
        if (Build.VERSION.SDK_INT >= 23 && !pu.isExternalStorageAvailable()) {
            pu.requestExternalStoragePermission(0);
        } else {
            send();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 0)  send();
    }

    private Uri logToFile() throws IOException {
        String name = Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator + "pw.thedrhax.mosmetro.txt";
        File log = new File(name);

        FileWriter writer = new FileWriter(log);
        writer.write(Logger.read(Logger.LEVEL.DEBUG));
        writer.flush(); writer.close();

        return Uri.parse("file://" + name);
    }

    private void send() {
        try {
            email.putExtra(Intent.EXTRA_STREAM,
                    logToFile()
            );
        } catch (IOException ex) {
            Logger.log(Logger.LEVEL.DEBUG, ex);
            Logger.log(getString(R.string.error, getString(R.string.error_log_file)));
            email.putExtra(Intent.EXTRA_TEXT,
                    Logger.read(Logger.LEVEL.DEBUG)
            );
        }

        startActivity(Intent.createChooser(email, getString(R.string.report_choose_client)));
        finish();
    }
}
