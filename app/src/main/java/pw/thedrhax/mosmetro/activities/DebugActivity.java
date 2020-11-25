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

package pw.thedrhax.mosmetro.activities;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.LinkedList;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.services.ConnectionService;
import pw.thedrhax.util.Logger;

public class DebugActivity extends AppCompatActivity {
    public static final String INTENT_VIEW_ONLY = "view_only";

    // UI Elements
    private RecyclerView text_messages;
    private LogAdapter text_messages_adapter;
    private Button button_connect;

    // Status variables
    private boolean service_running = false;

    // Receivers
    private BroadcastReceiver service_state;
    private IntentFilter service_filter;

    // Callbacks
    private Logger.Callback logger_callback;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.debug_activity);

        button_connect = (Button) findViewById(R.id.button_connect);
        service_state = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                service_running = intent.getBooleanExtra("RUNNING", false);
                button_connect.setText(service_running ?
                        getString(R.string.stop) : getString(R.string.retry)
                );
            }
        };
        service_filter = new IntentFilter("pw.thedrhax.mosmetro.event.ConnectionService");

        text_messages = (RecyclerView)findViewById(R.id.text_messages);
        text_messages_adapter = new LogAdapter();
        text_messages.setAdapter(text_messages_adapter);
        text_messages.setLayoutManager(new LinearLayoutManager(this));
        text_messages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (Build.VERSION.SDK_INT >= 14) {
                    text_messages_adapter.autoscroll = !recyclerView.canScrollVertically(1);
                }
            }
        });

        logger_callback = new Logger.Callback() {
            @Override
            public void log(Logger.LEVEL level, String message) {
                text_messages_adapter.refresh();
            }
        };

        Intent intent = getIntent();

        boolean view_only = false;
        if (intent != null) {
            view_only = intent.getBooleanExtra(INTENT_VIEW_ONLY, false);
        }

        if (!ConnectionService.isRunning() && !view_only){
            button_connect(null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(service_state);
        Logger.unregisterCallback(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(service_state, service_filter);
        Logger.registerCallback(this, logger_callback);
        text_messages_adapter.refresh();

        // Get initial ConnectionService state (not very accurate)
        service_state.onReceive(this,
                new Intent().putExtra("RUNNING", ConnectionService.isRunning())
        );
    }

    // ActionBar Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.debug_activity, menu);

        // Show back button in menu
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share:
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

                if (!settings.getBoolean("pref_share_warning", false))
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.pref_share_warning)
                            .setMessage(R.string.pref_share_warning_summary)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Logger.share(DebugActivity.this);
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .setNeutralButton(R.string.do_not_warn, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    PreferenceManager.getDefaultSharedPreferences(DebugActivity.this)
                                            .edit().putBoolean("pref_share_warning", true).apply();
                                    Logger.share(DebugActivity.this);
                                }
                            })
                            .show();
                else
                    Logger.share(this);

                return true;

            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.action_clear:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.log_wipe_confirmation)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Logger.wipe();
                                text_messages_adapter.refresh();
                            }
                        })
                        .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                return;
                            }
                        })
                        .show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        startActivity(
                new Intent(this, SettingsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        );
        finish();
    }

    public void button_connect (final View view) {
        Intent service = new Intent(this, ConnectionService.class);
        if (service_running)
            service.setAction("STOP");
        else
            service.putExtra("debug", true);
        startService(service);
    }

    // Handle debug log checkbox
    public void show_debug_log (View view) {
        text_messages_adapter.showDebug(((CheckBox)view).isChecked());
    }

    public class LogAdapter extends RecyclerView.Adapter<LogAdapter.ViewHolder> {
        private boolean show_debug = false;
        private boolean autoscroll = true;

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView mTextView;

            ViewHolder(TextView view) {
                super(view);
                mTextView = view;
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView view = new TextView(parent.getContext());
            view.setTypeface(Typeface.MONOSPACE);
            view.setTextColor(Color.BLACK);
            view.setTextSize(14);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.mTextView.setText(getDataset().get(position));
        }

        @Override
        public int getItemCount() {
            return getDataset().size();
        }

        private LinkedList<String> getDataset() {
            return Logger.read(show_debug ? Logger.LEVEL.DEBUG : Logger.LEVEL.INFO);
        }

        void showDebug(boolean enabled) {
            show_debug = enabled;
            refresh();
        }

        void refresh() {
            notifyDataSetChanged();

            if (autoscroll) {
                text_messages.scrollToPosition(text_messages_adapter.getItemCount() - 1);
            }
        }
    }
}
