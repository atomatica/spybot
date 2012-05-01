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

public class MonitorActivity extends Activity {
    public static final byte maintain = 0;
    public static final byte led1 = 10;
    public static final byte led2 = 11;
    public static final byte led3 = 12;
    public static final byte servo = 20;
    public static final byte rightMotor = 21;
    public static final byte leftMotor = 22;
    
    private static final String TAG = "SpybotActivity";
    
    private SpybotApplication mApplication;

    private SerialPort mSerialPort;
    private InputStream mSerialInput;
    private OutputStream mSerialOutput;
    private SerialReceivingThread mSerialReceivingThread;
    private SerialSendingThread mSerialSendingThread;

    private NetworkThread mNetworkThread;
    
    private TextView mSerialText;
    private TextView mNetworkText;
    
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
                        mSerialOutput.write(maintain);
                        mSerialOutput.write((byte)0x0);
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

    private class NetworkThread extends Thread {
        private Socket mNetworkPort;
        private ObjectInputStream mNetworkInput;
        private ObjectOutputStream mNetworkOutput;
        
        @Override
        public void run() {
        	Log.d(TAG, "Network thread started");
        	
            try {
                mNetworkPort = new Socket(InetAddress.getByName("atomatica.com"), 9103);
                mNetworkOutput = new ObjectOutputStream(mNetworkPort.getOutputStream());
                mNetworkOutput.flush();
                mNetworkInput = new ObjectInputStream(mNetworkPort.getInputStream());
                String message = "";
                do {
                    if (mNetworkInput != null && mNetworkOutput != null) {
                        try {
                            message = (String)mNetworkInput.readObject();
                            String[] tokens = message.split("\\s");
                            byte value = 0;
                            if (tokens.length == 2) {
                                value = (byte)Integer.parseInt(tokens[1]);
                            }
                            
                            if (tokens[0].equals("LED1")) {
                                mSerialOutput.write(led1);
                                mSerialOutput.write(value);
                            }

                            else if (tokens[0].equals("LED2")) {
                                mSerialOutput.write(led2);
                                mSerialOutput.write(value);
                            }

                            else if (tokens[0].equals("LED3")) {
                                mSerialOutput.write(led3);
                                mSerialOutput.write(value);
                            }
                            
                            else if (tokens[0].equals("SERVO")) {
                                mSerialOutput.write(servo);
                                mSerialOutput.write(value);
                            }
                            
                            else if (tokens[0].equals("LEFTMOTOR")) {
                                mSerialOutput.write(leftMotor);
                                mSerialOutput.write(value);
                            }

                            else if (tokens[0].equals("RIGHTMOTOR")) {
                                mSerialOutput.write(rightMotor);
                                mSerialOutput.write(value);
                            }
                            
                            else if (tokens[0].equals("KEEPALIVE")) {
                                mSerialOutput.write(maintain);
                                mSerialOutput.write((byte)0x0);
                            }
                            
                            mNetworkOutput.writeObject("ACK");
                            mNetworkOutput.flush();
                        }
                        
                        catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                } while (!message.equals("TERMINATE"));
            }

            // server closed connection
            catch (EOFException e) {
                e.printStackTrace();
            }

            catch (IOException e) {
                e.printStackTrace();
            }
            
            finally {
                closeConnection();
            }
        }
        
        private void closeConnection() {
            // close network connection
            try {
                mNetworkInput.close();
                mNetworkOutput.close();
                mNetworkPort.close();
            }
            catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.monitor);
        setTitle("Spybot Client");
    	
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

        // setup network connection
        mNetworkThread = new NetworkThread();
        mNetworkThread.start();
        
        mSerialText = (TextView)findViewById(R.id.serial_text);
        mNetworkText = (TextView)findViewById(R.id.network_text);
        
        final Button led1Button = (Button)findViewById(R.id.led1_button);
        led1Button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mSerialOutput != null) {
                    try {
                        mSerialOutput.write(led1);
                        mSerialOutput.write((byte)0xaa);
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
                if (mSerialText != null) {
                    mSerialText.setText(new String(buffer, 0, size));
                }
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
