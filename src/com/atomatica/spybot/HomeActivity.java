package com.atomatica.spybot;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.app.Service;

public class HomeActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        final Button controllerButton = (Button)findViewById(R.id.controller_button);
        controllerButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, ControllerActivity.class));
            }
        });

        final Button monitorButton = (Button)findViewById(R.id.monitor_button);
        monitorButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startService(new Intent(HomeActivity.this, SpybotService.class));
                startActivity(new Intent(HomeActivity.this, MonitorActivity.class));
            }
        });
        
        final Button preferencesButton = (Button)findViewById(R.id.preferences_button);
        preferencesButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, SpybotPreferences.class));
            }
        });

        final Button aboutButton = (Button)findViewById(R.id.about_button);
        aboutButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
                builder.setTitle("About");
                builder.setMessage(R.string.about_msg);
                builder.show();
            }
        });

        final Button quitButton = (Button)findViewById(R.id.quit_button);
        quitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	stopService(new Intent(HomeActivity.this, SpybotService.class));
                HomeActivity.this.finish();
            }
        });
    }
}
