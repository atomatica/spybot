package com.atomatica.spybot;

import java.io.*;
import java.net.*;
import java.security.InvalidParameterException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.util.Log;
import android_serialport_api.SerialPort;

public class ControllerActivity extends Activity {
    private static final String TAG = "Spybot Controller";
    
    private SpybotApplication mApplication;

    private SerialPort mSerialPort;
    private InputStream mSerialInput;
    private OutputStream mSerialOutput;
    private SerialReceivingThread mSerialReceivingThread;
    private SerialSendingThread mSerialSendingThread;
    
    private class SerialReceivingThread extends Thread {
        @Override
        public void run() {
        	Log.d(TAG, "Serial receiving thread started");
        	
            while(!isInterrupted()) {
                if (mSerialInput != null) {
                    try {
                        byte[] buffer = new byte[64];
                        int size = mSerialInput.read(buffer);
                        if (size > 0) {
                            onSerialDataReceived(buffer, size);
                        }
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }
        }
    }

    private class SerialSendingThread extends Thread {
        @Override
        public void run() {
        	Log.d(TAG, "Serial sending thread started");
        	
            while (!isInterrupted()) {
                if (mSerialOutput != null) {
                    try {
                        mSerialOutput.write(Protocol.maintain);
                        mSerialOutput.write((byte)0);
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                }

                try {
                    Thread.sleep(500);
                }
                catch (InterruptedException e) {
                }
            }
        }
    }
    
    private void DisplayError(int resourceId) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Error");
        b.setMessage(resourceId);
        b.setPositiveButton("OK", new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                ControllerActivity.this.finish();
            }
        });
        b.show();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.controller);
        setTitle("Spybot Controller");
    	
        mApplication = (SpybotApplication)getApplication();
        
        // setup serial connection
        try {
            mSerialPort = mApplication.getSerialPort();
            mSerialInput = mSerialPort.getInputStream();
            mSerialOutput = mSerialPort.getOutputStream();

            // create a serial receiving thread
            mSerialReceivingThread = new SerialReceivingThread();
            mSerialReceivingThread.start();
            
            // create a serial sending thread
            mSerialSendingThread = new SerialSendingThread();
            mSerialSendingThread.start();
        }
        
        catch (SecurityException e) {
            DisplayError(R.string.error_security);
            Log.e(TAG, "Security error");
        }

        catch (InvalidParameterException e) {
            DisplayError(R.string.error_configuration);
            Log.e(TAG, "Configuration error");
        }
        
        catch (IOException e) {
            DisplayError(R.string.error_unknown);
            Log.e(TAG, "IO Error");
        }
        
        final Button forwardButton = (Button)findViewById(R.id.forward_button);
        forwardButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mSerialOutput != null) {
                    try {
                        mSerialOutput.write(Protocol.leftMotor);
                        mSerialOutput.write((byte)0);
                        mSerialOutput.write(Protocol.rightMotor);
                        mSerialOutput.write((byte)0);
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }
        });
        
        final Button reverseButton = (Button)findViewById(R.id.reverse_button);
        reverseButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mSerialOutput != null) {
                    try {
                        mSerialOutput.write(Protocol.leftMotor);
                        mSerialOutput.write((byte)255);
                        mSerialOutput.write(Protocol.rightMotor);
                        mSerialOutput.write((byte)255);
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }
        });
        
        final Button leftButton = (Button)findViewById(R.id.left_button);
        leftButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mSerialOutput != null) {
                    try {
                        mSerialOutput.write(Protocol.leftMotor);
                        mSerialOutput.write((byte)255);
                        mSerialOutput.write(Protocol.rightMotor);
                        mSerialOutput.write((byte)0);
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }
        });
        
        final Button rightButton = (Button)findViewById(R.id.right_button);
        rightButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mSerialOutput != null) {
                    try {
                        mSerialOutput.write(Protocol.leftMotor);
                        mSerialOutput.write((byte)0);
                        mSerialOutput.write(Protocol.rightMotor);
                        mSerialOutput.write((byte)255);
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }
        });
        
        final Button stopButton = (Button)findViewById(R.id.stop_button);
        stopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mSerialOutput != null) {
                    try {
                        mSerialOutput.write(Protocol.leftMotor);
                        mSerialOutput.write((byte)128);
                        mSerialOutput.write(Protocol.rightMotor);
                        mSerialOutput.write((byte)128);
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }
        });
    }
    
    protected void onSerialDataReceived(final byte[] buffer, final int size) {
        runOnUiThread(new Runnable() {
            public void run() {
            }
        });
    }

    @Override
    public void onDestroy() {
        // close serial connection
        if (mSerialReceivingThread != null) {
            mSerialReceivingThread.interrupt();
        }

        if (mSerialSendingThread != null) {
            mSerialSendingThread.interrupt();
        }

        mApplication.closeSerialPort();
        mSerialPort = null;

        super.onDestroy();
    }
}
