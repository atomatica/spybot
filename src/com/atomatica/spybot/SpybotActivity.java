package com.atomatica.spybot;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import java.util.Arrays;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android_serialport_api.SerialPort;

public class SpybotActivity extends Activity {
	private SpybotApplication mApplication;
	private SerialPort mSerialPort;
    private InputStream mInputStream;
	private OutputStream mOutputStream;
	private ReceivingThread mReceivingThread;
    private SendingThread mSendingThread;
	
    private EditText mReception;
    
    byte[] mBuffer;
    byte maintain = 0;
    byte led1 = 10;
    byte led2 = 11;
    byte led3 = 12;
    byte servo = 20;
    byte rightM = 21;
    byte leftM = 22;
    
	private class ReceivingThread extends Thread {
		@Override
		public void run() {
			super.run();
			while(!isInterrupted()) {
				try {
					byte[] buffer = new byte[64];
					if (mInputStream == null) {
					    return;
					}
					int size = mInputStream.read(buffer);
					if (size > 0) {
						onDataReceived(buffer, size);
					}
				}
				catch (IOException e) {
					e.printStackTrace();
					return;
				}
			}
		}
	}
	
    private class SendingThread extends Thread {
        @Override
        public void run() {
            /*while (!isInterrupted()) {
                try {
                    if (mOutputStream != null) {
                        mOutputStream.write(mBuffer);
                    }
                    else {
                        return;
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }*/
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
        
        mBuffer = new byte[2];
        mBuffer[0] = maintain;
        mBuffer[1] = maintain;
        
        try {
            mSerialPort = mApplication.getSerialPort();
            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();

            // create a receiving thread
            mReceivingThread = new ReceivingThread();
            mReceivingThread.start();
            
            // create a sending thread
            mSendingThread = new SendingThread();
            mSendingThread.start();
        }
        
        catch (SecurityException e) {
            DisplayError(R.string.error_security);
        }
        
        catch (IOException e) {
            DisplayError(R.string.error_unknown);
        }
        
        catch (InvalidParameterException e) {
            DisplayError(R.string.error_configuration);
        }
        
        mReception = (EditText)findViewById(R.id.EditTextReception);
        
        EditText Emission = (EditText)findViewById(R.id.EditTextEmission);
        Emission.setOnEditorActionListener(new OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                int i;
                CharSequence t = v.getText();
                char[] text = new char[t.length()];
                for (i=0; i<t.length(); i++) {
                    text[i] = t.charAt(i);
                }
                try {
                    mOutputStream.write(new String(text).getBytes());
                    mOutputStream.write('\n');
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return false;
            }
        });
        
        final Button led1Button = (Button)findViewById(R.id.led1_button);
        led1Button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mBuffer[0] = led2;
                mBuffer[1] = (byte)0xaa;
                for (int i = 0; i < 100; i++) {
                    try {
                        if (mOutputStream != null) {
                            mOutputStream.write(mBuffer);
                        }
                        else {
                            return;
                        }
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }
        });
    }
    
    protected void onDataReceived(final byte[] buffer, final int size) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (mReception != null) {
                    mReception.append(new String(buffer, 0, size));
                }
            }
        });
    }
    
	@Override
	protected void onDestroy() {
		if (mReceivingThread != null) {
			mReceivingThread.interrupt();
		}

        if (mSendingThread != null) {
            mSendingThread.interrupt();
        }
        
		mApplication.closeSerialPort();
		mSerialPort = null;
		super.onDestroy();
	}
}
