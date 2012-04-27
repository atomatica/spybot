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
import android_serialport_api.SerialPort;

public class SpybotActivity extends Activity {
    public static final byte maintain = 0;
    public static final byte led1 = 10;
    public static final byte led2 = 11;
    public static final byte led3 = 12;
    public static final byte servo = 20;
    public static final byte rightM = 21;
    public static final byte leftM = 22;
    
	private SpybotApplication mApplication;
	
	private SerialPort mSerialPort;
    private InputStream mSerialInput;
	private OutputStream mSerialOutput;
	private SerialReceivingThread mSerialReceivingThread;
    private SerialSendingThread mSerialSendingThread;
	
    private Socket mNetworkPort;
    private ObjectInputStream mNetworkInput;
    private ObjectOutputStream mNetworkOutput;
    private NetworkReceivingThread mNetworkReceivingThread;
    
    private TextView mSerialText;
    private TextView mNetworkText;
    
	private class SerialReceivingThread extends Thread {
		@Override
		public void run() {
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
            }
        }
    }
    
    private class NetworkReceivingThread extends Thread {
        @Override
        public void run() {
            while(!isInterrupted()) {
                if (mNetworkInput != null) {
                    try {
                        String message = (String)mNetworkInput.readObject();
                        onNetworkDataReceived(message);
                    }
                    
                    catch (ClassNotFoundException e) {
                        e.printStackTrace();
                        return;
                    }
                    
                    catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
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
				SpybotActivity.this.finish();
			}
		});
		b.show();
	}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.spybot);
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
        }

        catch (InvalidParameterException e) {
            DisplayError(R.string.error_configuration);
        }
        
        catch (IOException e) {
            DisplayError(R.string.error_unknown);
        }
        
        // setup network connection
        try {
            mNetworkPort = new Socket(InetAddress.getByName("atomatica.com"), 9103);
            mNetworkInput = new ObjectInputStream(mNetworkPort.getInputStream());
            mNetworkOutput = new ObjectOutputStream(mNetworkPort.getOutputStream());
        }
        
        // server closed connection
        catch (EOFException e) {
            DisplayError(R.string.error_unknown);
        }
        
        catch (IOException e) {
            DisplayError(R.string.error_unknown);
        }
        
        mSerialText = (TextView)findViewById(R.id.serial_text);
        mNetworkText = (TextView)findViewById(R.id.network_text);
        
        final Button led1Button = (Button)findViewById(R.id.led1_button);
        led1Button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mSerialOutput != null) {
                    try {
                        //mSerialOutput.write(led1);
                        //mSerialOutput.write((byte)0xaa);
                        mNetworkOutput.writeObject("hello");
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
    
    protected void onNetworkDataReceived(final String message) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (mNetworkText != null) {
                    mNetworkText.setText(message);
                }
            }
        });
    }
    
	@Override
	protected void onDestroy() {
	    // close network connection
	    try {
	        mNetworkInput.close();
	        mNetworkOutput.close();
	        mNetworkPort.close();
	    }
	    catch (IOException e) {
            DisplayError(R.string.error_unknown);
	    }
	    
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
