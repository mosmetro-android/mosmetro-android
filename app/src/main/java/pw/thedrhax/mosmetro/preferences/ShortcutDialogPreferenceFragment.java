package pw.thedrhax.mosmetro.preferences;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import androidx.preference.PreferenceDialogFragmentCompat;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.activities.ConnectionServiceActivity;
import pw.thedrhax.mosmetro.activities.DebugActivity;
import pw.thedrhax.mosmetro.activities.SettingsActivity;

import static android.app.Activity.RESULT_OK;

public class ShortcutDialogPreferenceFragment extends PreferenceDialogFragmentCompat {
    private Context context;
    private CheckBox check_background;
    private CheckBox check_force;
    private CheckBox check_log;
    private CheckBox check_stop;

    public ShortcutDialogPreferenceFragment(ShortcutDialogPreference preference) {
        Bundle bundle = new Bundle(1);
        bundle.putString("key", preference.getKey());
        setArguments(bundle);

        context = preference.getContext();
    }

    @Override
    protected View onCreateDialogView(Context context) {
        View inflate = LayoutInflater
                .from(getContext())
                .inflate(R.layout.shortcut_dialog_preference, null);

        check_background = (CheckBox) inflate.findViewById(R.id.check_background);
        check_force = (CheckBox) inflate.findViewById(R.id.check_force);
        check_log = (CheckBox) inflate.findViewById(R.id.check_log);
        check_stop = (CheckBox) inflate.findViewById(R.id.check_stop);

        check_background.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                check_background();
            }
        });

        check_log.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                check_log();
            }
        });

        check_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                check_stop();
            }
        });

        return inflate;
    }

    public void check_background() {
        boolean checked = check_background.isChecked();
        check_force.setEnabled(checked);
        check_log.setEnabled(!checked);
        check_stop.setEnabled(!checked);

        if (!checked) {
            check_force.setChecked(false);
        }
    }

    public void check_log() {
        boolean checked = check_log.isChecked();
        check_force.setEnabled(!checked);
        check_background.setEnabled(!checked);
        check_stop.setEnabled(!checked);
    }

    public void check_stop() {
        boolean checked = check_stop.isChecked();
        check_force.setEnabled(!checked);
        check_log.setEnabled(!checked);
        check_background.setEnabled(!checked);
    }

    private String getShortcutName() {
        if (check_background.isChecked()) {
            return context.getString(R.string.in_background);
        } else if (check_stop.isChecked()) {
            return context.getString(R.string.stop);
        } else if (check_log.isChecked()) {
            return context.getString(R.string.log);
        } else {
            return context.getString(R.string.connect);
        }
    }

    private Class getActivityClass() {
        if (check_background.isChecked() || check_stop.isChecked()) {
            return ConnectionServiceActivity.class;
        } else {
            return DebugActivity.class;
        }
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            Intent result = new Intent();

            Intent shortcut_intent = new Intent(((SettingsActivity) context), getActivityClass())
                    .putExtra("force", check_force.isChecked())
                    .putExtra("stop", check_stop.isChecked())
                    .putExtra("view_only", check_log.isChecked());

            result.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcut_intent);
            result.putExtra(Intent.EXTRA_SHORTCUT_NAME, getShortcutName());
            result.putExtra(
                    Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(((SettingsActivity) context), R.drawable.ic_launcher)
            );

            if ("android.intent.action.CREATE_SHORTCUT".equals(((SettingsActivity) context).getIntent().getAction())) {
                ((SettingsActivity) context).setResult(RESULT_OK, result);
            } else {
                result.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                ((SettingsActivity) context).sendBroadcast(result);
            }
        }
    }
}
