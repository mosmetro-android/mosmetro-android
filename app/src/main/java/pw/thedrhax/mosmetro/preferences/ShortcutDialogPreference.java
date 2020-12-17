package pw.thedrhax.mosmetro.preferences;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;

public class ShortcutDialogPreference extends DialogPreference {
    private Context context;

    public ShortcutDialogPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.context = context;
    }

    public ShortcutDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
    }

    public ShortcutDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public ShortcutDialogPreference(Context context) {
        super(context);
        this.context = context;
    }

    public Context getContext() {
        return context;
    }
}
