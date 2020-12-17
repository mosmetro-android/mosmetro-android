package pw.thedrhax.mosmetro.preferences;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.preference.PreferenceManager;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.activities.SettingsActivity;

public class ThemeDialogPreferenceFragment extends PreferenceDialogFragmentCompat {
    public static final int THEME_DEFAULT = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;
    public static final int THEME_OLED = 3;
    private Context context;
    private SharedPreferences settings;
    int selected;
    int selection;

    public ThemeDialogPreferenceFragment(ThemeDialogPreference preference) {
        Bundle bundle = new Bundle(1);
        bundle.putString("key", preference.getKey());
        setArguments(bundle);

        context = preference.getContext();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        final String[] list = context.getResources().getStringArray(R.array.theme_options);

        selected = settings.getInt("pref_theme", 0);
        if (selected < 0 || selected > list.length) {
            selected = 0;
        }
        selection = selected;

        builder.setTitle(R.string.pref_theme);
        builder.setSingleChoiceItems(list, selected, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                selection = i;
            }
        });
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            if (selection == selected) {
                onDialogClosed(false);
                return;
            }
            settings.edit().putInt("pref_theme", selection).apply();
            ((SettingsActivity) context).recreate();
        }
    }
}
