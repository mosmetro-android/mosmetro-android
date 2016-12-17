package pw.thedrhax.mosmetro.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import pw.thedrhax.mosmetro.services.ConnectionService;

public class ConnectionServiceActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(this, ConnectionService.class).putExtras(getIntent()));
        finish();
    }
}
