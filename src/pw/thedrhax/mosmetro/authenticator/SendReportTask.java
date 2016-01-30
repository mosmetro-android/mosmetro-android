package pw.thedrhax.mosmetro.authenticator;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.widget.EditText;
import pw.thedrhax.mosmetro.R;
import pw.thedrhax.util.Logger;

public class SendReportTask extends AsyncTask<Void, Void, Void> {
    private Context context;
    private Logger report;

    private boolean isConnected;

    private AuthenticatorStat connection;
    private AlertDialog.Builder result_dialog;

    public SendReportTask (Context context, Logger report) {
        this.context = context;
        this.report = report;

        connection = new AuthenticatorStat(context, false);

        result_dialog = new AlertDialog.Builder(context)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {}
                });
    }

    @Override
    protected Void doInBackground(Void... params) {
        isConnected = connection.isConnected();
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);

        final EditText input = new EditText(context);

        AlertDialog.Builder message_dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.share)
                .setMessage(R.string.share_info)
                .setView(input)
                .setPositiveButton(R.string.send, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        connection.report(report, input.getText().toString());
                        result_dialog
                                .setTitle(R.string.share_ok)
                                .setMessage(R.string.share_ok_info)
                                .show();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {}
                });

        if (isConnected) {
            message_dialog.show();
        } else {
            result_dialog
                    .setTitle(R.string.share_fail)
                    .setMessage(R.string.share_fail_info)
                    .show();
        }
    }
}