package com.atomatica.spybot;

import java.io.*;
import java.net.*;
import java.security.InvalidParameterException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.util.Log;
import android_serialport_api.SerialPort;

public class MonitorActivity extends Activity {
    protected SpybotApplication mApplication;
    private TextView mSerialText;
    private TextView mNetworkText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.monitor);
        setTitle("Spybot Monitor");
    	
        mApplication = (SpybotApplication)getApplication();
        
        mSerialText = (TextView)findViewById(R.id.serial_text);
        mNetworkText = (TextView)findViewById(R.id.network_text);
        final Button disconnectButton = (Button)findViewById(R.id.disconnect_button);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopService(new Intent(MonitorActivity.this, SpybotService.class));
            }
        });
    }
    
    protected void onSerialDataReceived(final byte[] buffer, final int size) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (mSerialText != null) {
                    mSerialText.setText(new String(buffer, 0, size));
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void DisplayError(int resourceId) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Error");
        b.setMessage(resourceId);
        b.setPositiveButton("OK", new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                MonitorActivity.this.finish();
            }
        });
        b.show();
    }
}
