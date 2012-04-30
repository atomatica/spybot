package com.atomatica.spybot;

import java.io.*;
import java.net.*;
import java.security.InvalidParameterException;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android_serialport_api.SerialPort;

public class SpybotService extends Service {
    public static final byte maintain = 0;
    public static final byte led1 = 10;
    public static final byte led2 = 11;
    public static final byte led3 = 12;
    public static final byte servo = 20;
    public static final byte rightMotor = 21;
    public static final byte leftMotor = 22;
    
    private static final String TAG = "SpybotService";
    
    private SpybotApplication mApplication;

    private SerialPort mSerialPort;
    private InputStream mSerialInput;
    private OutputStream mSerialOutput;
    private SerialReceivingThread mSerialReceivingThread;
    private SerialSendingThread mSerialSendingThread;

    private NetworkThread mNetworkThread;
    
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
        private BufferedReader mNetworkInput;
        private PrintWriter mNetworkOutput;
        
        @Override
        public void run() {
        	Log.d(TAG, "Network thread started");
        	
            try {
                mNetworkPort = new Socket(InetAddress.getByName("atomatica.com"), 9103);
                mNetworkInput = new BufferedReader(new InputStreamReader(mNetworkPort.getInputStream()));
                mNetworkOutput = new PrintWriter(mNetworkPort.getOutputStream(), true);
                String message = "";
                do {
                    if (mNetworkInput != null && mNetworkOutput != null) {
                        message = mNetworkInput.readLine();
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

                        mNetworkOutput.println("ACK");
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
    }
    
    
    @Override
	public void onStart(Intent intent, int startid) {
	}

    @Override
    public void onCreate() {
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
    }
    
    protected void onSerialDataReceived(final byte[] buffer, final int size) {
        /*runOnUiThread(new Runnable() {
            public void run() {
                if (mSerialText != null) {
                    mSerialText.setText(new String(buffer, 0, size));
                }
            }
        });*/
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

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
}
